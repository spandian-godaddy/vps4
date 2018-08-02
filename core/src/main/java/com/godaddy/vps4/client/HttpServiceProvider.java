package com.godaddy.vps4.client;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;

import org.apache.http.client.HttpClient;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.hfs.config.Config;


abstract public class HttpServiceProvider<T> {

    private static final Logger logger = LoggerFactory.getLogger(HttpServiceProvider.class);

    static volatile KeyManager keyManager;

    @Inject Config config;
    @Inject JacksonJsonProvider jacksonJsonProvider;
    private final String baseUrlConfigPropName;
    private String baseUrl;
    private final Class<T> serviceClass;
    private T serviceClient;

    public HttpServiceProvider(String baseUrlConfigPropName,
                               Class<T> serviceClass) {
        this.baseUrlConfigPropName = baseUrlConfigPropName;
        this.serviceClass = serviceClass;
    }

    public T get() {
        if (serviceClient == null) {
            this.baseUrl = config.get(baseUrlConfigPropName);
            serviceClient = buildClient();
        }

        return serviceClient;
    }

    List<ClientRequestFilter> getRequestFilters() {
       return new ArrayList<>();
    }

    List<ClientResponseFilter> getResponseFilters() {
        return new ArrayList<>();
    }

    final void registerRequestFilters(Client client) {
        for (ClientRequestFilter filter: getRequestFilters()) {
            client.register(filter);
        }
    }

    final void registerResponseFilters(Client client) {
        for (ClientResponseFilter filter: getResponseFilters()) {
            client.register(filter);
        }
    }

    KeyManager[] getKeyManagers() {
        return null;
    }

    TrustManager[] getTrustManagers() {
        TrustManager[] trustManagers = { trustManager };
        return trustManagers;
    }

    private boolean isOverSSL() {
        return baseUrl.startsWith("https");
    }

    private SSLConnectionSocketFactory getSSLConnectionSocketFactory()
        throws KeyManagementException, NoSuchAlgorithmException {
        KeyManager[] keyManagers = getKeyManagers();
        TrustManager[] trustManagers = getTrustManagers();

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext,
                new String[] { "TLSv1.2" }, //limitation by delegation api only supports tlsv1
                null,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return sslsf;
    }

    private HttpClient getHttpClient(RegistryBuilder<ConnectionSocketFactory> registryBuilder) {
        PoolingHttpClientConnectionManager connPool = getConnectionPool(registryBuilder);
        return HttpClients.custom()
                .setConnectionManager(connPool)
                .build();
    }

    private PoolingHttpClientConnectionManager getConnectionPool(RegistryBuilder<ConnectionSocketFactory> registryBuilder) {
        // configure a connection pool
        PoolingHttpClientConnectionManager connPool = new PoolingHttpClientConnectionManager(registryBuilder.build());
        connPool.setMaxTotal(500);
        connPool.setDefaultMaxPerRoute(250);
        return connPool;
    }

    private T buildClient() {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        registryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);

        if (isOverSSL()) {
            try {
                registryBuilder.register("https", getSSLConnectionSocketFactory());
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        HttpClient httpClient = getHttpClient(registryBuilder);
        ResteasyClient client = new ResteasyClientBuilder()
                .httpEngine(new ApacheHttpClient4Engine(httpClient))
                .build();

        logger.debug("building HTTP client to service {} at base url {}", serviceClass.getName(), baseUrl);

        client.register(jacksonJsonProvider);
        registerRequestFilters(client);
        registerResponseFilters(client);
        ResteasyWebTarget target = client.target(baseUrl);

        return target.proxyBuilder(serviceClass).classloader(getClass().getClassLoader()).build();
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
}
