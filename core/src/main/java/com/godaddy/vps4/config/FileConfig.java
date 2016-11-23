package com.godaddy.vps4.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.util.Charsets;

public class FileConfig {

    private static final Logger logger = LoggerFactory.getLogger(FileConfig.class);

    public static Config readFromClasspath(Config parent, String...paths) throws IOException {

        BasicConfig fileConfig = new BasicConfig(parent);
        for (String path : paths) {
            readProperties(path, fileConfig);
        }
        return fileConfig;
    }

    public static void readProperties(String resourcePath, BasicConfig config) throws IOException {

        InputStream is = ConfigProvider.class.getResourceAsStream(resourcePath);
        if (is == null) {
            logger.warn("Config resource not found: {}", resourcePath);
            return;
        }

        logger.info("loading config: {}", resourcePath);

        Properties props = new Properties();
        props.load(new InputStreamReader(is, Charsets.UTF8));

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            logger.trace(" {} => {}", key, value);
            config.set(key, value);
        }
    }

}
