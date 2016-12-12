package com.godaddy.vps4.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivateKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.BasicConfig;
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.config.ConfigNode;
import com.godaddy.hfs.config.ConfigNodeReader;
import com.godaddy.hfs.config.FileConfig;
import com.godaddy.hfs.config.SystemPropertyConfig;
import com.godaddy.hfs.config.ZooKeeperConfig;
import com.godaddy.hfs.crypto.PEMFile;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.Environment;
import com.godaddy.vps4.tools.EncryptionConfig;

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

        // TODO enable when zookeeper config is being pushed
        if (false) { //ZooKeeperClient.isConfigured()) {
            // if zookeeper is setup, use only it
            logger.info("ZooKeeper client configuration present, using ZooKeeper for configuration");

            try {
                config = ConfigNodeReader.toConfig(
                        new ZooKeeperConfig(ZooKeeperClient.getInstance()).readConfig("/config/vps4"),
                        config);
            } catch (Exception e) {
                throw new RuntimeException("Error building ZooKeeper config", e);
            }

        } else {
            logger.info("No ZooKeeper client configured, using local file config");

            Environment env = Environment.CURRENT;
            logger.info("configuration environment: {}", env);

            String basePath = "/com/godaddy/vps4/config";

            try {
                //PrivateKey privateKey = readPrivateKey(env);

                FileConfig fileConfig = new FileConfig();

                // read configs, renaming the root node to "" so that the root of the path
                // isn't "base" or "dev/test/..."
                ConfigNode baseNode = fileConfig.readConfig(basePath + "/base").rename("");
                ConfigNode envNode = fileConfig.readConfig(basePath + "/" + env.getLocalName()).rename("");

                // TODO decrypt baseNode, envNode
                // remove 'blah.ext.enc' nodes, replace in ConfigNode tree with 'blah.ext.unenc'
                // ConfigNodeReader should have intelligence to look for a '.unenc' file and
                // - for properties files, merge the config values together
                // - for data files, overwrite the existing with the .unenc

                // merge baseNode, envNode, encEnvNode in overriding order
                // (properties files are automatically merged since we're writing to a common BasicConfig)
                // (data files overwrite)
                BasicConfig mergedConfig = new BasicConfig(config);

                if (baseNode != null) {
                    ConfigNodeReader.read(baseNode, mergedConfig);
                }

                if (envNode != null) {
                    ConfigNodeReader.read(envNode, mergedConfig);
                }

                config = mergedConfig;

            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new SystemPropertyConfig(config);
    }

    static PrivateKey readPrivateKey(Environment env) throws Exception {

        // get the private key for this environment
        String privateKeyPath = "/vps4." + env.getLocalName() + ".priv.pem";
        InputStream is = EncryptionConfig.class.getResourceAsStream(privateKeyPath);
        if (is == null) {
            return null;
        }
        return PEMFile.readPEM(new BufferedReader(new InputStreamReader(is))).getPrivateKey();
    }
}
