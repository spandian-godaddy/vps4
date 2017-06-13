package com.godaddy.vps4.hfs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.util.ThreadLocalRequestId;

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

        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory>create();
        registryBuilder.register("http", PlainConnectionSocketFactory.INSTANCE);

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

                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                        sslContext,
                        new String[] { "TLSv1.2" }, //limitation by delegation api only supports tlsv1
                        null,
                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                registryBuilder.register("https", sslsf);
            }

        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }

        // configure a connection pool
        PoolingHttpClientConnectionManager connPool = new PoolingHttpClientConnectionManager(registryBuilder.build());
        connPool.setMaxTotal(500);
        connPool.setDefaultMaxPerRoute(250);

        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connPool)
                .build();

        Client client = new ResteasyClientBuilder()
                .httpEngine(new ApacheHttpClient4Engine(httpClient))
                .build();

        logger.debug("building HTTP client to service {} at base url {}", serviceClass.getName(), baseUrl);

        client.register(jacksonJsonProvider);

        client.register(new ClientResponseFilter() {
            @Override
            public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
                if (!responseContext.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                    StringBuilder errMsg = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(responseContext.getEntityStream()));
                    String line;
                    while((line = reader.readLine()) != null){
                        errMsg.append(line);
                    }
                    logger.error("Error response with status {} returned. Response body: {}.",
                            Integer.toString(responseContext.getStatus()), StringUtils.left(errMsg.toString(), 1024));
                }
            }
        });

        // add a header to all HFS requests indicating the request ID
        client.register(new ClientRequestFilter() {

            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {

                String requestId = ThreadLocalRequestId.get();
                if (requestId != null) {
                    requestContext.getHeaders().add("X-Request-Id", requestId);
                }
            }
        });

        ResteasyWebTarget target = (ResteasyWebTarget) client.target(baseUrl);

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
