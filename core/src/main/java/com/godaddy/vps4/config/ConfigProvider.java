package com.godaddy.vps4.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Charsets;

public class ConfigProvider implements Provider<Config> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigProvider.class);

    public ConfigProvider() {

    }

    @Override
    public Config get() {

        // populate Config object
        SystemPropertyConfig config = new SystemPropertyConfig();

        String environment = System.getProperty("vps4.env", "local");
        logger.info("configuration environment: {}", environment);

        // require loading base configuration
        try {
            readProperties("/vps4.properties", config);
        } catch(IOException e) {
            throw new IllegalStateException(e);
        }

        // optionally load environment-specific configuration
        String envConfigPath = "/vps4." + environment + ".properties";
        try {
            readProperties(envConfigPath, config);
        } catch (IOException e) {
            logger.warn("Environment-specific config file not found: {}", envConfigPath);
        }

        return config;
    }

    protected void readProperties(String resourcePath, BasicConfig config) throws IOException {

        InputStream is = ConfigProvider.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }

        Properties props = new Properties();
        props.load(new InputStreamReader(is, Charsets.UTF8));

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            logger.trace(" {} => {}", key, value);
            config.set(key, value);
        }
    }

}
