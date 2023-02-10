package com.godaddy.hfs.crypto;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.io.Charsets;
import com.godaddy.hfs.io.IOUtils;


public class PEMFile {

    private static final Logger logger = LoggerFactory.getLogger(PEMFile.class);

    private final X509Certificate[] certChain;

    private final PublicKey publicKey;

    private final PrivateKey privateKey;

    public PEMFile(X509Certificate[] certChain, PublicKey publicKey, PrivateKey privateKey) {
        this.certChain = certChain;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public X509Certificate[] getCertChain() {
        return certChain;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static PEMFile readPEM(Reader ... readers) {
        
        List<X509Certificate> certChain = new ArrayList<>();
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
    	
        for(Reader reader : readers) {

	        try (PEMParser pemReader = new PEMParser(reader)) {
	        	
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
	                    
	                } else if (pemObject instanceof SubjectPublicKeyInfo) {
	                    SubjectPublicKeyInfo pubKeyInfo = (SubjectPublicKeyInfo)pemObject;
	                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	                    EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyInfo.getEncoded());
	                    publicKey = keyFactory.generatePublic(publicKeySpec);
	                    
	                } else {
	                    logger.debug("unknown PEM object: {}" + pemObject.toString());
	                }
	            }
	        } catch (IOException | CertificateException | NoSuchAlgorithmException | InvalidKeySpecException e) {
	            // TODO
	            throw new RuntimeException(e);
	        }
        }
        
        return new PEMFile(certChain.toArray(new X509Certificate[certChain.size()]), publicKey, privateKey);
    }

    public static PEMFile readPEM(Path ... files) {
        
        List<Reader> readers = new ArrayList<>(files.length);
        
        try {
            for(Path file : files) {
                 readers.add(Files.newBufferedReader(file, Charsets.UTF8));
            }
            return readPEM(readers.toArray(new Reader[readers.size()]));

        } catch(IOException e) {
            throw new RuntimeException("Failed reading PEM files: " + e.getMessage(), e);
        }
        finally {
            IOUtils.closeQuietly(readers);
        }
    }

    @Override
    public String toString() {
        return "PEMFile [certChain=" + Arrays.toString(certChain) + ", publicKey=" + publicKey + ", privateKey="
                + privateKey + "]";
    }

}
