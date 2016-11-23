package com.godaddy.vps4.hfs;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.hfs.crypto.PEMFile;
import com.godaddy.vps4.util.Charsets;
import com.google.common.io.ByteStreams;

public class HfsKeyManagerBuilder {

    private static final Logger logger = LoggerFactory.getLogger(HfsKeyManagerBuilder.class);

    private static class HfsKeyManager implements X509KeyManager {

        final X509Certificate[] certChain;

        final PrivateKey privateKey;

        public HfsKeyManager(X509Certificate[] certChain, PrivateKey privateKey) {
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



    protected X509Certificate[] readCertificateChain(InputStream certStream) {
        try (InputStream is = new BufferedInputStream(certStream)) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certs = certFactory.generateCertificates(is);

            X509Certificate[] certChain = new X509Certificate[certs.size()];
            int certIndex = 0;

            for (Certificate cert : certs) {
                certChain[certIndex++] = (X509Certificate) cert;
            }
            return certChain;
        }
        catch (IOException | CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    protected PrivateKey readPrivateKey(InputStream keyStream) {
        try (InputStream is = new BufferedInputStream(keyStream)) {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            // read the key into memory
            ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
            int b = -1;
            while ((b = is.read()) != -1) {
                bos.write(b);
            }

            byte[] keyBytes = bos.toByteArray();

            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static KeyManager newKeyManager(Config config) {

        // TODO Get the key from Zoookeeper based on environment
        String keyPath = config.get("hfs.client.key");
        String certPath = config.get("hfs.client.cert");

        InputStream isKey = HfsClientProvider.class.getResourceAsStream(keyPath);
        InputStream isCrt = HfsClientProvider.class.getResourceAsStream(certPath);

        try {
            byte[] bytesKey = ByteStreams.toByteArray(isKey);
            byte[] bytesCrt = ByteStreams.toByteArray(isCrt);

            PEMFile privatePemFile = PEMFile.readPEM(new InputStreamReader(
                    new ByteArrayInputStream(bytesKey),
                    Charsets.UTF8));
            PEMFile publicPemFile = PEMFile.readPEM(new InputStreamReader(
                    new ByteArrayInputStream(bytesCrt),
                    Charsets.UTF8));

            logger.info("client private key: {}", privatePemFile.getPrivateKey());
            logger.info("client public key: {}", publicPemFile.getCertChain().toString());

            return new HfsKeyManager(publicPemFile.getCertChain(), privatePemFile.getPrivateKey());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
