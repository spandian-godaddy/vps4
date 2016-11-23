package com.godaddy.vps4.hfs;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.vps4.config.Config;

@Singleton
public class HfsClientProvider<T> implements Provider<T> {

    private static final Logger logger = LoggerFactory.getLogger(HfsClientProvider.class);

    private final Class<T> serviceClass;

    static volatile KeyManager keyManager;

    @Inject
    Config config;

    @Inject
    JacksonJsonProvider jacksonJsonProvider;

    @Inject
    public HfsClientProvider(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    public T get() {
        String baseUrl = config.get("hfs.base.url");

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();

        try {

            if (baseUrl.startsWith("https")) {
                if (keyManager == null) {
                    keyManager = HfsKeyManagerBuilder.newKeyManager(config);
                }
                // we're connecting to an SSL endpoint, and we have
                // client certificates, so wire that up
                KeyManager[] keyManagers = { keyManager };
                TrustManager[] trustManagers = { trustManager };

                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(keyManagers, trustManagers, new SecureRandom());

                clientBuilder = clientBuilder.sslContext(sslContext).hostnameVerifier(INSECURE_HOSTNAME_VERIFIER);
            }

        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }

        Client client = clientBuilder.build();

        logger.debug("building HTTP client to service {} at base url {}", serviceClass.getName(), baseUrl);

        client.register(jacksonJsonProvider);

        // client.register(new ErrorResponseFilter());

        ResteasyWebTarget target = (ResteasyWebTarget) client.target(baseUrl);

        return (T) target.proxy(serviceClass);
    }

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

    private static final HostnameVerifier INSECURE_HOSTNAME_VERIFIER = new HostnameVerifier() {

        @Override
        public boolean verify(String arg0, SSLSession arg1) {
            return true;
        }

    };
}
