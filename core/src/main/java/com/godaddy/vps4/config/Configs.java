package com.godaddy.vps4.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.ClasspathConfigBuilder;
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.config.ConfigBuilder;
import com.godaddy.hfs.config.ConfigNode;
import com.godaddy.hfs.config.SystemPropertyConfig;
import com.godaddy.hfs.config.ZooKeeperConfig;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.Environment;

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

        try {
            config = new SystemPropertyConfig();
            String configMode = System.getProperty("vps4.config.mode", "zk").toLowerCase();

            if (configMode.equals("zk") && ZooKeeperClient.isConfigured()) {
                // if zookeeper is setup, use only it
                logger.info("ZooKeeper client configuration present, using ZooKeeper for configuration");

                config = ConfigBuilder.merge(config,
                        new ZooKeeperConfig(ZooKeeperClient.getInstance())
                            .readConfig("/config/vps4"));

            } else {
                logger.info("No ZooKeeper client configured, using local file config");

                Environment env = Environment.CURRENT;
                logger.info("configuration environment: {}", env);

                String basePath = "/com/godaddy/vps4/config/";

                List<ConfigNode> nodes = new ArrayList<>();

                ClasspathConfigBuilder classpathBuilder = new ClasspathConfigBuilder(
                    basePath + "base",
                    basePath + env.getLocalName()
                );

                classpathBuilder.build(nodes);

                config = ConfigBuilder.merge(config, nodes);
            }

            return new SystemPropertyConfig(config);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
