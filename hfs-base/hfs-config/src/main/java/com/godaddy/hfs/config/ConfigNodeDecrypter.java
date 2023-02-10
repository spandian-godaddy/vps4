package com.godaddy.hfs.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigNodeDecrypter implements ConfigNodeVisitor {

    private static final Logger logger = LoggerFactory.getLogger(ConfigNodeDecrypter.class);

    private final PrivateKey privateKey;

    public ConfigNodeDecrypter(PrivateKey privateKey) {
        Objects.requireNonNull(privateKey, "Config private key is required");
        this.privateKey = privateKey;
    }

    public ConfigNode visit(ConfigNode node) {
        String name = node.getName();

        if (name.endsWith(".enc") && node.content != null) {
            // unencrypt reader with environment-specific private key
            //logger.info("found encrypted config at: {}", path);

            try {

                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);

                // wrap the existing input stream
                CipherInputStream cis = new CipherInputStream(new ByteArrayInputStream(node.content), cipher);

                byte[] decryptedContent = IOUtils.toByteArray(cis);

                return new ConfigNode(node.getName(), decryptedContent);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return node;
    }
}
