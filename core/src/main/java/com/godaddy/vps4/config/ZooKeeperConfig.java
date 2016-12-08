package com.godaddy.vps4.config;



import java.io.Closeable;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Charsets;

public class ZooKeeperConfig extends BasicConfig implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConfig.class);

    private static final String DEFAULT_BASE_PATH = "/config";

    private final CuratorFramework zkClient;

    private final String basePath;

    private final TreeCache cache;

    public ZooKeeperConfig(CuratorFramework zkClient) {
        this(zkClient, DEFAULT_BASE_PATH, null);
    }

    public ZooKeeperConfig(CuratorFramework zkClient, Config parent) {
        this(zkClient, DEFAULT_BASE_PATH, parent);
    }

    public ZooKeeperConfig(CuratorFramework zkClient, String basePath) {
        this(zkClient, basePath, null);
    }

    public ZooKeeperConfig(CuratorFramework zkClient, String basePath, Config parent) {
        super(parent);
        this.zkClient = zkClient;
        this.basePath = basePath;

        readConfig(basePath);

        cache = TreeCache.newBuilder(zkClient, basePath)
                .setCacheData(true)
                .build();

        try {
            cache.start();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start ZK tree cache", e);
        }

        // add listener to re-parse config data
        //
        // TODO need to know which property keys correspond to which
        //      z-node so we can remove all the keys previously associated
        //      when we receive an updated/removed notification for that znode

    }

    @Override
    public void close() {
        cache.close();
    }

    public String getBasePath() {
        return basePath;
    }

    protected void readConfig(String basePath) {

        try {
            readConfig("", basePath);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public byte[] getData(String path) {
        try {
            return zkClient.getData().forPath(path);
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error reading configuration data", e);
        }
    }

    protected List<String> getChildren(String path) throws Exception {
        return zkClient.getChildren().forPath(path);
    }

    protected void readConfig(String parentPrefix, String path) throws Exception {

        logger.debug("reading node {}  (parent prefix: {})", path, parentPrefix);

        // read data of node
        //
        byte[] utf8Data = getData(path);
        if (utf8Data == null) {
            logger.warn("No config found in ZooKeeper at path: {}", path);
            return;
        }

        String data = new String(utf8Data, Charsets.UTF8);

        Properties props = new Properties();
        props.load(new StringReader(data));

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            key = parentPrefix + key;
            logger.trace(" {} => {}", key, value);
            properties.put(key, value);
        }

        // recurse into child nodes
        List<String> childNodes = getChildren(path);

        for (String childNode : childNodes) {
            String childPrefix = parentPrefix.length() == 0
                                    ? childNode + "."
                                    : parentPrefix + "." + childNode + ".";
            readConfig(childPrefix, path + '/' + childNode);
        }
    }

}
