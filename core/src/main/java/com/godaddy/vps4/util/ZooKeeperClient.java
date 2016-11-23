package com.godaddy.vps4.util;


import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class ZooKeeperClient {

    public static final String ZK_HOSTS_PROPERTY = "vps4.zk.hosts";

    public static final String ZK_TIMEOUT_PROPERTY = "vps4.zk.timeout";

    public static final String DEFAULT_ZK_TIMEOUT = "50";

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperClient.class);


    private static CuratorFramework curatorFramework;

    protected ZooKeeperClient() {
        // defeat instantiation
    }

    public static boolean isConfigured() {
        return !Strings.isNullOrEmpty(System.getProperty(ZK_HOSTS_PROPERTY));
    }

    public static synchronized CuratorFramework getInstance() {
        if (curatorFramework == null) {
            String hosts = System.getProperty(ZK_HOSTS_PROPERTY);
            if (hosts == null) {
                throw new IllegalStateException("ZooKeeper hosts not configured: " + ZK_HOSTS_PROPERTY);
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
