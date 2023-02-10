package com.godaddy.hfs.config;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperConfig {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConfig.class);

    private final CuratorFramework zkClient;

    public ZooKeeperConfig(CuratorFramework zkClient) {
        this.zkClient = zkClient;
    }


    public ConfigNode readConfig(String basePath) throws Exception {

        return readConfig(basePath, "");
    }

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

    protected ConfigNode readConfig(String parentPath, String name) throws Exception {

        logger.debug("reading node {}/{}", parentPath, name);

        String path = parentPath;
        if (name.length() > 0) {
            path += '/' + name;
        }

        byte[] content = getData(path);

        ConfigNode node = new ConfigNode(name, content);

        // recurse into child nodes
        List<String> childPaths = getChildren(path);

        for (String childName : childPaths) {

            ConfigNode childNode = readConfig(path, childName);
            node.getChildren().add(childNode);
        }

        return node;
    }

}
