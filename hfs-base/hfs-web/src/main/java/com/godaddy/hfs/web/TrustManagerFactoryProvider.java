package com.godaddy.hfs.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.crypto.PEMFile;
import com.godaddy.hfs.io.Charsets;

@Singleton
public class TrustManagerFactoryProvider implements Provider<TrustManagerFactory> {

	private static final Logger logger = LoggerFactory.getLogger(TrustManagerFactoryProvider.class);

	private static final String PKIX_ALGORITHM = "PKIX";

    private static final String ROOT_CERT_CONFIG_KEY = "https.rootcert";

	private final Config config;

	@Inject
	public TrustManagerFactoryProvider(Config config) {
		this.config = config;
	}

	@Override
	public TrustManagerFactory get() {			 
    
        PEMFile pemFile = PEMFile.readPEM(
                new InputStreamReader(
                        new ByteArrayInputStream(config.getData(ROOT_CERT_CONFIG_KEY)),
                        Charsets.UTF8));
        
		KeyStore trustStore = createTrustStore(pemFile);
		TrustManagerFactory factory = createTrustManagerFactory(trustStore);

		return factory;
	}
	
	private KeyStore createTrustStore(PEMFile pemFile) {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null);
			for(X509Certificate cert : pemFile.getCertChain()) {
				trustStore.setCertificateEntry(cert.getSubjectX500Principal().getName(), cert);
			}
			return trustStore;
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			throw new RuntimeException("Failed populating trust store: " + e.getMessage(), e);
		}
	}
	
	private TrustManagerFactory createTrustManagerFactory(KeyStore hfsRootKeyStore) {
		try {
			TrustManagerFactory factory = TrustManagerFactory.getInstance(PKIX_ALGORITHM);
			factory.init(hfsRootKeyStore);
			return factory;
		} catch (NoSuchAlgorithmException | KeyStoreException e) {
			throw new RuntimeException("Building TrustManagerFactory failed: " + e.getMessage(), e);
		}
	}

}
