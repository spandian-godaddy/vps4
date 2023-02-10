package com.godaddy.hfs.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigBuilder.class);

    public static Config merge(Config parent, ConfigNode...nodes) throws IOException {
        return merge(parent, Arrays.asList(nodes));
    }

    public static Config merge(Config parent, List<ConfigNode> nodes) throws IOException {

        BasicConfig mergedConfig = new BasicConfig(parent);

        List<ConfigNode> unencryptedNodes = new ArrayList<>();

        for (ConfigNode node : nodes) {
            if (node != null) {

                unencryptedNodes.add(extractUnencrypted(node));

                logger.info("merging config node: {}", node);
                ConfigNodeReader.read(node, mergedConfig);
            }
        }

        for (ConfigNode node : unencryptedNodes) {
            logger.info("merging unencrypted config node: {}", node);
            ConfigNodeReader.read(node, mergedConfig);
        }

        return mergedConfig;
    }

    /**
     * prune '*.unenc' nodes from the given tree,
     *   collect them into another tree (after removing .unenc suffix)
     *
     * @param node
     * @return
     */
    static ConfigNode extractUnencrypted(ConfigNode node) {
        ConfigNode unencryptedNode = new ConfigNode(node.getName(), node.getContent());

        Iterator<ConfigNode> childIter = node.getChildren().iterator();
        while (childIter.hasNext()) {
            ConfigNode child = childIter.next();
            if (child.getName().endsWith(".unenc")) {
                // found unencrypted node, add as child of parent unencrypted node
                unencryptedNode.getChildren().add(
                        new ConfigNode(
                                child.getName().substring(0, child.getName().length() - ".unenc".length()),
                                child.getContent()));

                // and remove it from the current tree
                childIter.remove();

            } else if (child.getName().endsWith(".enc")) {

                // remove .enc files from the initial config
                childIter.remove();

            } else if (child.getChildren().size() > 0) {
                // only add 'directory' child config nodes
                // (ignore non-.unenc, non-parent nodes)
                unencryptedNode.getChildren().add(extractUnencrypted(child));
            }
        }
        return unencryptedNode;
    }
}
