package com.godaddy.vps4.scheduler.core.config;

import com.godaddy.hfs.config.ClasspathConfigBuilder;
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.config.ConfigBuilder;
import com.godaddy.hfs.config.ConfigNode;
import com.godaddy.hfs.config.SystemPropertyConfig;
import com.godaddy.hfs.config.ZooKeeperConfig;
import com.godaddy.hfs.zookeeper.ZooKeeperClient;
import com.godaddy.vps4.scheduler.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Configs {

    private static final Logger logger = LoggerFactory.getLogger(Configs.class);

    private static volatile Config config;
    static {
        config = buildConfig();
    }

    public static Config getInstance() {
        return config;
    }

    static Boolean shouldLoadConfigFromZookeeper() {
        String configMode = System.getProperty("vps4.scheduler.config.mode", "zk").toLowerCase();
        return configMode.equals("zk") && ZooKeeperClient.isConfigured();
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
            if (shouldLoadConfigFromZookeeper()) {
                logger.info("ZooKeeper client configuration present, using ZooKeeper for configuration");

                String zkConfigBasePath = "/config/vps4/scheduler";
                config = ConfigBuilder.merge(
                    config, new ZooKeeperConfig(ZooKeeperClient.getInstance()).readConfig(zkConfigBasePath));

            } else {
                logger.info("Not using zookeeper configuration, using local file config");

                Environment env = Environment.CURRENT;
                logger.info("configured environment: {}", env);

                String basePath = "/com/godaddy/vps4/scheduler/config";
                String baseConfigPath = String.format("%s/%s", basePath, "base");
                String envConfigPath = String.format("%s/%s", basePath, env.getLocalName());
                ClasspathConfigBuilder classpathBuilder = new ClasspathConfigBuilder(baseConfigPath, envConfigPath);

                List<ConfigNode> nodes = new ArrayList<>();
                classpathBuilder.build(nodes);
                config = ConfigBuilder.merge(config, nodes);
            }

            return new SystemPropertyConfig(config);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
