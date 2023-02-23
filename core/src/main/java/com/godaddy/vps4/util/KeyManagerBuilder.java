package com.godaddy.vps4.util;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(KeyManagerBuilder.class);

    public static class CertificateKeyManager implements X509KeyManager {

        final X509Certificate[] certChain;

        final PrivateKey privateKey;

        public CertificateKeyManager(X509Certificate[] certChain, PrivateKey privateKey) {
            this.certChain = certChain;
            this.privateKey = privateKey;
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers,
                                        Socket socket) {
            return "vertical-client";
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers,
                                        Socket socket) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return certChain;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return privateKey;
        }
    };

    public static KeyManager newKeyManager(Config config, String clientCertKeyPath, String clientCertPath) {

        String keyPath = config.get(clientCertKeyPath);
        String certPath = config.get(clientCertPath);

        logger.info("reading HFS key/cert from {}/{}", keyPath, certPath);

        try {
            byte[] bytesKey = config.getData(keyPath);
            byte[] bytesCrt = config.getData(certPath);

            Objects.requireNonNull(bytesKey, "HFS key is required");
            Objects.requireNonNull(bytesCrt, "HFS cert is required");

            PEMFile privatePemFile = PEMFile.readPEM(new InputStreamReader(
                    new ByteArrayInputStream(bytesKey),
                    Charsets.UTF8));
            PEMFile publicPemFile = PEMFile.readPEM(new InputStreamReader(
                    new ByteArrayInputStream(bytesCrt),
                    Charsets.UTF8));

            logger.info("client private key: {}", privatePemFile.getPrivateKey());
            logger.info("client public key: {}", Arrays.toString(publicPemFile.getCertChain()));

            return new CertificateKeyManager(publicPemFile.getCertChain(), privatePemFile.getPrivateKey());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
