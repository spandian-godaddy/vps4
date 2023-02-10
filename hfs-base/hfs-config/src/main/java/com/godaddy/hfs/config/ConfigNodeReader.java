package com.godaddy.hfs.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.io.Charsets;

public class ConfigNodeReader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigNodeReader.class);

    public static ConfigNode visit(ConfigNode node, ConfigNodeVisitor visitor) {

        ConfigNode newNode = visitor.visit(node);
        if (newNode != null) {
            visitChildren(newNode.children, visitor);
        }
        return newNode;
    }

    public static void visitChildren(List<ConfigNode> children, ConfigNodeVisitor visitor) {

        int childCount = children.size();
        for(int i=0; i<childCount; i++) {
            ConfigNode child = children.get(i);
            ConfigNode newChild = visit(child, visitor);

            if (newChild != child) {
                children.set(i, newChild);
            }
        }

    }

    public static Config toConfig(ConfigNode node, Config parent) throws IOException {
        BasicConfig config = new BasicConfig(parent);
        read(node, config);
        return config;
    }

    public static void read(ConfigNode node, BasicConfig config) throws IOException {
        read(node, "", "", config);
    }

    public static void read(ConfigNode node, String parentConfigPath, String parentDataPath, BasicConfig config) throws IOException {

        if (node.getChildren().size() > 0) {
            String childConfigPath = parentConfigPath.length() > 0 ? parentConfigPath + '.' : "";
            childConfigPath += node.getName();

            String childDataPath = parentDataPath.length() > 0 ? parentDataPath + '/' : "";
            childDataPath += node.getName();

            for (ConfigNode childNode : node.getChildren()) {
                read(childNode, childConfigPath, childDataPath, config);
            }

        } else {

            if (node.getName().endsWith(".properties")) {

                if (node.getContent() == null) {
                    return;
                }

                // load properties into our map
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(node.content), Charsets.UTF8))) {

                    Properties props = new Properties();
                    props.load(reader);

                    String keyPrefix = parentConfigPath;
                    if (keyPrefix.length() > 0) {
                        keyPrefix += '.';
                    }
                    if (!node.getName().equals("config.properties")) {
                        // configPath = 'a.b'
                        // loading c.properties
                        // should prefix properties as 'a.b.c'
                        keyPrefix += node.getName().substring(0, ".properties".length());
                    }

                    for (String key : props.stringPropertyNames()) {
                        String value = props.getProperty(key);
                        key = keyPrefix + key;
                        logger.trace(" {} => {}", key, value);
                        config.set(key, value);
                    }
                }

            } else {

                // everything else goes in data

                if (node.content != null) {
                    String dataPath = parentDataPath;
                    if (dataPath.length() > 0) {
                        dataPath += "/";
                    }
                    dataPath += node.getName();
                    logger.trace(" {} ({} bytes)", dataPath,  node.content.length);

                    config.data.put(dataPath, node.content);
                }


            }
        }

    }

}
