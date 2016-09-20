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
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.hfs.crypto.PEMFile;
import com.godaddy.vps4.util.Charsets;
import com.google.common.io.ByteStreams;

@Singleton
public class HfsClientProvider<T> implements Provider<T> {

    private static final Logger logger = LoggerFactory.getLogger(HfsClientProvider.class);

    private final Class<T> serviceClass;
    
    final KeyManager keyManager;

    @Inject Config config;

    @Inject JacksonJsonProvider jacksonJsonProvider;

    @Inject
    public HfsClientProvider(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
        this.keyManager = newKeyManager();
    }
    
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

    private static final X509TrustManager trustManager = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    };

	protected X509Certificate[] readCertificateChain(InputStream certStream)
	{
		try(InputStream is = new BufferedInputStream(certStream))
    	{
    		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
    		Collection<? extends Certificate> certs = certFactory.generateCertificates(is);

    		X509Certificate[] certChain = new X509Certificate[certs.size()];
    		int certIndex = 0;

    		for (Certificate cert : certs)
    		{
    			certChain[certIndex++] = (X509Certificate)cert;
    		}
    		return certChain;
    	}
		catch(IOException|CertificateException e)
		{
			throw new RuntimeException(e);
		}
	}

	protected PrivateKey readPrivateKey(InputStream keyStream)
	{
		try(InputStream is = new BufferedInputStream(keyStream))
    	{
    		KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    		// read the key into memory
    		ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
    		int b = -1;
    		while ((b = is.read()) != -1)
    		{
    			bos.write(b);
    		}

    		byte[] keyBytes = bos.toByteArray();

    		EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

    		return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    	}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	protected static KeyManager newKeyManager() {

		InputStream is = HfsClientProvider.class.getResourceAsStream("/com/godaddy/businesshosting/hfs/diablo.hfs.stage.pem");
		if (is == null) {
			logger.info("Cannot find client certificate");
			return null;
		}


        try {
        	byte[] bytes = ByteStreams.toByteArray(is);

            PEMFile privatePemFile = PEMFile.readPEM(new InputStreamReader(
            		new ByteArrayInputStream(bytes),
                    Charsets.UTF8));
            PEMFile publicPemFile = PEMFile.readPEM(new InputStreamReader(
                    new ByteArrayInputStream(bytes),
                    Charsets.UTF8));

            logger.info("client private key: {}", privatePemFile.getPrivateKey());
            logger.info("client public key: {}", publicPemFile.getCertChain().toString());

            return new HfsKeyManager(publicPemFile.getCertChain(), privatePemFile.getPrivateKey());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER = new HostnameVerifier() {

        @Override
        public boolean verify(String arg0, SSLSession arg1) {
            return true;
        }

    };

    @Override
    public T get() {
        String baseUrl = config.get("hfs.base.url");

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();

		try {
			clientBuilder = clientBuilder.withConfig(new ClientConfig());

			if (baseUrl.startsWith("https")) {
				if (keyManager == null) {
					throw new IllegalStateException("HTTPS connection requested, but we have no client certificates");
				}
				// we're connecting to an SSL endpoint, and we have
				// client certificates, so wire that up
				KeyManager[] keyManagers = { keyManager };
				TrustManager[] trustManagers = { trustManager };

				SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
				sslContext.init(keyManagers, trustManagers, new SecureRandom());

				clientBuilder = clientBuilder.sslContext(sslContext).hostnameVerifier(INSECURE_HOSTNAME_VERIFIER);
			}

		} catch (Throwable t) {
			throw new RuntimeException(t);
		}

		Client client = clientBuilder.build();

		logger.debug("building HTTP client to service {} at base url {}", serviceClass.getName(), baseUrl);

		client.register(jacksonJsonProvider);

		// client.register(new ErrorResponseFilter());

		ResteasyWebTarget target = (ResteasyWebTarget) client.target(baseUrl);

		return (T) target.proxy(serviceClass);
    }
}
