package com.godaddy.vps4.config;

import java.io.IOException;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.ZooKeeperClient;

public class ConfigProvider implements Provider<Config> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigProvider.class);

    @Override
    public Config get() {

        Config config = new SystemPropertyConfig();

        if (ZooKeeperClient.isConfigured()) {
            // if zookeeper is setup, use only it
            logger.info("ZooKeeper client configuration present, using ZooKeeper for configuration");

            config = new ZooKeeperConfig(ZooKeeperClient.getInstance(), "/config/vps4", config);

        } else {

            logger.info("No ZooKeeper client configured, using configuration files on the classpath");

            String environment = System.getProperty("vps4.env", "local");
            logger.info("configuration environment: {}", environment);

            try {
                config = FileConfig.readFromClasspath(config, "/vps4.properties", "/vps4." + environment + ".properties");
            } catch(IOException e) {
                throw new IllegalStateException(e);
            }
        }

        return config;
    }

}
