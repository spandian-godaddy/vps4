package com.godaddy.vps4.hfs.crypto;

import java.io.IOException;
import java.io.Reader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PEMFile {

    private static final Logger logger = LoggerFactory.getLogger(PEMFile.class);


    private final X509Certificate[] certChain;

    private final PrivateKey privateKey;

    public PEMFile(X509Certificate[] certChain, PrivateKey privateKey) {
        this.certChain = certChain;
        this.privateKey = privateKey;
    }

    public X509Certificate[] getCertChain() {
        return certChain;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static PEMFile readPEM(Reader reader) {

        try (PEMParser pemReader = new PEMParser(reader)) {

            List<X509Certificate> certChain = new ArrayList<>();
            PrivateKey privateKey = null;

            Object pemObject = null;
            while ((pemObject = pemReader.readObject()) != null) {

                if (pemObject instanceof X509CertificateHolder) {
                    X509CertificateHolder certHolder = (X509CertificateHolder)pemObject;

                    X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);

                    certChain.add(cert);

                } else if (pemObject instanceof PrivateKeyInfo) {

                    PrivateKeyInfo privateKeyInfo = (PrivateKeyInfo)pemObject;

                    privateKey = new JcaPEMKeyConverter().getPrivateKey(privateKeyInfo);


                } else if (pemObject instanceof PEMKeyPair) {
                    PEMKeyPair pemKeyPair = (PEMKeyPair)pemObject;

                    KeyPair keyPair = new JcaPEMKeyConverter().getKeyPair(pemKeyPair);

                    privateKey = keyPair.getPrivate();

                } else {
                    logger.debug("unknown PEM object: {}" + pemObject.toString());
                }
            }

            return new PEMFile(certChain.toArray(new X509Certificate[certChain.size()]), privateKey);

        } catch (IOException|CertificateException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }

}
