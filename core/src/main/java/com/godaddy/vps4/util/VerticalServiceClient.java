package com.godaddy.vps4.util;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.vps4.config.Config;
import com.godaddy.vps4.config.ConfigProvider;

public class VerticalServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(VerticalServiceClient.class);
    
    private static final Config config = new ConfigProvider().get();
    
    public static <T> T newClient(Class<T> serviceClass) {
    	String baseUrl = config.get("hfs.base.url");
    	logger.debug("Base URL = " + baseUrl);
    	return newClient(baseUrl, serviceClass);
    }

    public static <T> T newClient(String baseUrl, Class<T> serviceClass) {

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();

//        try {
//            clientBuilder = clientBuilder.withConfig(new ClientConfig());
//
//            if (baseUrl.startsWith("https")) {
//                if (keyManager == null) {
//                    throw new IllegalStateException("HTTPS connection requested, but we have no client certificates");
//                }
//                // we're connecting to an SSL endpoint, and we have
//                // client certificates, so wire that up
//                KeyManager[] keyManagers = { keyManager };
//                TrustManager[] trustManagers = { trustManager };
//
//                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
//                sslContext.init(keyManagers, trustManagers, new SecureRandom());
//
//                clientBuilder = clientBuilder
//                        .sslContext(sslContext)
//                        .hostnameVerifier(INSECURE_HOSTNAME_VERIFIER);
//            }
//
//        } catch (Throwable t) {
//            throw new RuntimeException(t);
//        }

        Client client = clientBuilder.build();

        logger.debug("building vertical client to service {} at base url {}", serviceClass.getName(), baseUrl);

        JacksonJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
        jsonProvider.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        client.register(jsonProvider);

        //client.register(new ErrorResponseFilter());

        ResteasyWebTarget target = (ResteasyWebTarget)client.target(baseUrl);

        return target.proxy(serviceClass);
    }
}