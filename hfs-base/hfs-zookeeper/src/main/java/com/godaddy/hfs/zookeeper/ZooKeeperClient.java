package com.godaddy.hfs.zookeeper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class ZooKeeperClient {

    public static final String ZK_HOSTS_PROPERTY = "hfs.zk.hosts";

    public static final String ZK_TIMEOUT_PROPERTY = "hfs.zk.timeout";

    public static final String DEFAULT_ZK_TIMEOUT = "50";

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperClient.class);


    private static CuratorFramework curatorFramework;

    protected ZooKeeperClient() {
        // defeat instantiation
    }

    static final Path filePath = Paths.get("/etc/sysconfig/zookeeper");

    static final Pattern ETC_PREFIX_PATTERN = Pattern.compile("^ZK_HOSTS=\"(.*)\"");

    static boolean checkIsConfigured() {
        return !Strings.isNullOrEmpty(System.getProperty(ZK_HOSTS_PROPERTY))
            || Files.exists(filePath);
    }

    static final boolean configured = checkIsConfigured();

    public static boolean isConfigured() {
        return configured;
    }

    static String readSysconfig(Path path) {
        if (Files.exists(path)) {
            try {
                return Files.lines(path)
                    .map(String::trim)
                    .filter( line -> {
                        return ETC_PREFIX_PATTERN.matcher(line).matches();
                    } )
                    .findFirst()
                        .map( line -> {
                            Matcher matcher = ETC_PREFIX_PATTERN.matcher(line);
                            return matcher.matches() ? matcher.group(1) : null;
                        } )
                        .orElse(null);
            } catch (Exception e) {
                logger.warn("Unable to read zookeeper config at " + path, e);
            }
        }
        logger.warn("Zookeeper config file not found at {}", path);
        return null;
    }

    public static synchronized CuratorFramework getInstance() {
        if (curatorFramework == null) {
            String hosts = System.getProperty(ZK_HOSTS_PROPERTY);
            if (hosts == null) {

                // if no system property is defined, attempt to read from 'filePath'
                hosts = readSysconfig(filePath);
            }

            if (hosts == null) {
                throw new IllegalStateException(
                        "ZooKeeper hosts configuration not found at " + ZK_HOSTS_PROPERTY
                        + " or by reading " + filePath);
            }

            int retryTimeout = Integer.parseInt(System.getProperty(ZK_TIMEOUT_PROPERTY, DEFAULT_ZK_TIMEOUT));

            logger.info("connecting to zookeeper at: {}", hosts);

            curatorFramework = newInstance(hosts, retryTimeout);
        }
        return curatorFramework;
    }

    /**
     *
     * @param hosts
     * @param retryTimeout - milliseconds between retries
     * @return
     */
    public static CuratorFramework newInstance(String hosts, int retryTimeout) {

        logger.info("building client to ZooKeeper hosts: {}", hosts);

        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(hosts, new RetryNTimes(5, retryTimeout));
        curatorFramework.start();

        try {
            curatorFramework.blockUntilConnected();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for ZooKeeper connection", e);
        }

        return curatorFramework;
    }

}
