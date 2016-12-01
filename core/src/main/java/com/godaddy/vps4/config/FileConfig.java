package com.godaddy.vps4.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivateKey;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Environment;
import com.godaddy.vps4.hfs.crypto.PEMFile;
import com.godaddy.vps4.tools.EncryptionConfig;
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


        Reader reader = null;
        if (resourcePath.endsWith(".enc.properties")) {
            // unencrypt reader with environment-specific private key
            logger.info("found encrypted properties at: {}", resourcePath);

            try {
                PrivateKey key = readPrivateKey();

                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, key);

                reader = new InputStreamReader(new CipherInputStream(is, cipher), Charsets.UTF8);
            } catch (Exception e) {
                throw new IOException(e);
            }

        } else {
            reader = new InputStreamReader(is, Charsets.UTF8);
        }

        props.load(reader);

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            logger.trace(" {} => {}", key, value);
            config.set(key, value);
        }
    }

    static PrivateKey readPrivateKey() throws Exception {

        String env = Environment.CURRENT.getLocalName();

        // get the private key for this environment
        String privateKeyPath = "/vps4." + env + ".priv.pem";
        InputStream is = EncryptionConfig.class.getResourceAsStream(privateKeyPath);
        if (is == null) {
            throw new Exception("Private key for environment " + env + " not found at: " + privateKeyPath);
        }
        PrivateKey privateKey = PEMFile.readPEM(new BufferedReader(new InputStreamReader(is))).getPrivateKey();
        if (privateKey == null) {
            throw new Exception("No private key found in resource: " + privateKeyPath);
        }
        return privateKey;
    }

}
