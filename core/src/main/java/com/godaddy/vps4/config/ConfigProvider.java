package com.godaddy.vps4.config;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Environment;
import com.godaddy.vps4.util.ZooKeeperClient;

public class ConfigProvider implements Provider<Config> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigProvider.class);

    @Override
    public Config get() {

        Config config = null;

        if (ZooKeeperClient.isConfigured()) {
            // if zookeeper is setup, use only it
            logger.info("ZooKeeper client configuration present, using ZooKeeper for configuration");

            config = new ZooKeeperConfig(ZooKeeperClient.getInstance(), "/config/vps4", config);

        } else {

            logger.info("No ZooKeeper client configured, using configuration files on the classpath");

            Environment environment = Environment.CURRENT;
            logger.info("configuration environment: {}", environment);

            String basePath = "/com/godaddy/vps4/config";

            try {
                config = FileConfig.readFromClasspath(config,
                        basePath + "/base/vps4.properties",
                        basePath + "/" + environment.getLocalName() + "/vps4.properties",
                        basePath + "/" + environment.getLocalName() + "/vps4.enc.properties");
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new SystemPropertyConfig(config);
    }

}
