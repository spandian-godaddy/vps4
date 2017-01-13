package com.godaddy.vps4.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.BasicConfig;
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.config.ConfigNode;
import com.godaddy.hfs.config.ConfigNodeReader;
import com.godaddy.hfs.config.FileConfig;
import com.godaddy.hfs.config.SystemPropertyConfig;
import com.godaddy.hfs.config.ZooKeeperConfig;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.Environment;
import com.godaddy.vps4.tools.EncryptionConfig;
import com.godaddy.vps4.tools.EncryptionUtil;

public class Configs {

    private static final Logger logger = LoggerFactory.getLogger(Configs.class);

    private static volatile Config config;
    static {
        config = buildConfig();
    }

    public static Config getInstance() {
        return config;
    }

    /**
     * Bootstrap the Config in layers.
     *
     * Overwrite the 'config' field as we bootstrap from system properties, through ZooKeeper/file config.
     *
     * That way, as the Config is built, any classes that are trying to use Configs.getInstance()
     *   (the Environment class, for instance) can use Configuration as it's configured at that
     *   stage in the config bootstrap.
     *
     * This (potentially bad) idea was implemented so that the circular dependency of
     *   - in order to know what environment we're in, we need to read the config that tells us
     *   - in order to build the config, we need to (maybe) load environment-specific files
     *
     * @return
     */
    static Config buildConfig() {

        config = new SystemPropertyConfig();

        if (ZooKeeperClient.isConfigured()) {
            // if zookeeper is setup, use only it
            logger.info("ZooKeeper client configuration present, using ZooKeeper for configuration");

            buildZooKeeperConfig();

        } else {
            logger.info("No ZooKeeper client configured, using local file config");

            buildFileSystemConfig();
        }

        return new SystemPropertyConfig(config);
    }

    static void buildZooKeeperConfig() {
        try {

            // get the symmetric key for this environment
            //Key key = readKeyFromClasspath("/vps4.key");

            ConfigNode configNode = new ZooKeeperConfig(ZooKeeperClient.getInstance())
                    .readConfig("/config/vps4");

            //ConfigNode decryptedConfig = decrypt(configNode, key);

            config = mergeConfigs(configNode); //, decryptedConfig);

        } catch (Exception e) {
            throw new RuntimeException("Error building ZooKeeper config", e);
        }
    }

    static void buildFileSystemConfig() {
        Environment env = Environment.CURRENT;
        logger.info("configuration environment: {}", env);

        String classpathPath = "/com/godaddy/vps4/config";
        URL configResource = Configs.class.getResource(classpathPath);
        if (configResource != null) {

            try {
                URI uri = configResource.toURI();
                Path basePath = null;
                try {
                    basePath = Paths.get(uri);
                } catch (FileSystemNotFoundException e) {
                    FileSystem fs = FileSystems.newFileSystem(uri, Collections.<String,Object>emptyMap());

                    basePath = fs.provider().getPath(uri);
                }

                if (Files.exists(basePath)) {

                    logger.info("reading config at base path: {}", basePath);

                    FileConfig fileConfig = new FileConfig();

                    // read configs, renaming the root node to "" so that the root of the path
                    // isn't "base" or "dev/test/..."
                    ConfigNode baseNode = fileConfig.readConfig(basePath.resolve("base")).rename("");
                    ConfigNode envNode = fileConfig.readConfig(basePath.resolve(env.getLocalName())).rename("");

                    // get the symmetric key for this environment
                    //Key key = readKeyFromClasspath("/vps4." + env.name().toLowerCase() + ".key");

                    // remove 'blah.ext.enc' nodes, replace in ConfigNode tree with 'blah.ext'
                    //ConfigNode decryptedConfig = decrypt(envNode, key);

                    // merge baseNode, envNode, encEnvNode in overriding order
                    // (properties files are automatically merged since we're writing to a common BasicConfig)
                    // (data files overwrite)
                    config = mergeConfigs(baseNode, envNode); //, decryptedConfig);

                } else {
                    logger.info("No config found at path: {}", basePath);
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            logger.info("No config found on classpath at: {}", classpathPath);
        }
    }

    private static Key readKeyFromClasspath(String symmetricKeyPath) throws IOException {
        InputStream is = EncryptionConfig.class.getResourceAsStream(symmetricKeyPath);
        if (is == null) {
            throw new IllegalStateException("No environment symmetric key found on classpath at " + symmetricKeyPath);
        }

        return EncryptionUtil.readAesKey(
                Base64.getDecoder().decode(
                        IOUtils.toByteArray(is)));
    }

    private static Config mergeConfigs(ConfigNode... nodes) throws IOException {

        BasicConfig mergedConfig = new BasicConfig(config);

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

    static ConfigNode decrypt(ConfigNode node, Key key) {

    	if (node.getContent() != null
			&& node.getName().endsWith(".enc")) {

    		// node is an encrypted content node
    		try {

                byte[] decryptedContent = EncryptionUtil.decryptAes(node.getContent(), key);

                String newName = node.getName().substring(0, node.getName().length() - ".enc".length());

                logger.info("decrypting {} => {}", node.getName(), newName);

                return new ConfigNode(newName, decryptedContent);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    	} else if (node.getChildren().size() > 0) {

    		ConfigNode newNode = new ConfigNode(node.getName());

    		for (ConfigNode child : node.getChildren()) {
    			ConfigNode newChild = decrypt(child, key);
    			if (newChild != null) {
    				newNode.getChildren().add(newChild);
    			}
    		}
    		return newNode;

    	}

    	// node isn't encrypted, and doesn't have any children (ignore)
		return null;
    }
}
