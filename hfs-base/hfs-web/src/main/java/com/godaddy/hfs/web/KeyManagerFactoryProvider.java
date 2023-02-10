package com.godaddy.hfs.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.KeyManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.crypto.PEMFile;
import com.godaddy.hfs.io.Charsets;

@Singleton
public class KeyManagerFactoryProvider implements Provider<KeyManagerFactory> {

	private static final Logger logger = LoggerFactory.getLogger(KeyManagerFactoryProvider.class);
	
	private static final String PKIX_ALGORITHM = "PKIX";
	private static final char[] KEYSTORE_PASSWORD = "nopassword".toCharArray();
	
    private static final String SERVER_CERT_CONFIG_KEY = "https.cert";
    private static final String SERVER_KEY_CONFIG_KEY = "https.key";

	private final Config config;

	@Inject
	public KeyManagerFactoryProvider(Config config) {
		this.config = config;
	}

	@Override
	public KeyManagerFactory get() {
		
		PEMFile pemFile = PEMFile.readPEM(
		        new InputStreamReader(
		                new ByteArrayInputStream(config.getData(SERVER_CERT_CONFIG_KEY)),
		                Charsets.UTF8),
		        new InputStreamReader(
		                new ByteArrayInputStream(config.getData(SERVER_KEY_CONFIG_KEY)),
		                Charsets.UTF8));
		
		KeyStore keyStore = createKeyStore(pemFile);
		KeyManagerFactory factory = createKeyManagerFactory(keyStore);

		return factory;
	}
	
	private KeyStore createKeyStore(PEMFile pemFile) {
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null);
			keyStore.setKeyEntry("server_cert", pemFile.getPrivateKey(), KEYSTORE_PASSWORD, pemFile.getCertChain());
			return keyStore;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new RuntimeException("Failed populating key store: " + e.getMessage(), e);
		}
	}
	
	private KeyManagerFactory createKeyManagerFactory(KeyStore hfsRootKeyStore) {
		try {
			KeyManagerFactory factory = KeyManagerFactory.getInstance(PKIX_ALGORITHM);
			factory.init(hfsRootKeyStore, KEYSTORE_PASSWORD);
			return factory;
		} catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
			throw new RuntimeException("Building KeyManagerFactory failed: " + e.getMessage(), e);
		}
	}

}
