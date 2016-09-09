package com.godaddy.vps4.hfs;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
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

    @Inject Config config;

    @Inject JacksonJsonProvider jacksonJsonProvider;

    @Inject
    public HfsClientProvider(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    public T get() {
        String baseUrl = config.get("hfs.base.url");

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();

//      try {
//          clientBuilder = clientBuilder.withConfig(new ClientConfig());
//
//          if (baseUrl.startsWith("https")) {
//              if (keyManager == null) {
//                  throw new IllegalStateException("HTTPS connection requested, but we have no client certificates");
//              }
//              // we're connecting to an SSL endpoint, and we have
//              // client certificates, so wire that up
//              KeyManager[] keyManagers = { keyManager };
//              TrustManager[] trustManagers = { trustManager };
//
//              SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
//              sslContext.init(keyManagers, trustManagers, new SecureRandom());
//
//              clientBuilder = clientBuilder
//                      .sslContext(sslContext)
//                      .hostnameVerifier(INSECURE_HOSTNAME_VERIFIER);
//          }
//
//      } catch (Throwable t) {
//          throw new RuntimeException(t);
//      }

      Client client = clientBuilder.build();

      logger.debug("building vertical client to service {} at base url {}", serviceClass.getName(), baseUrl);

      client.register(jacksonJsonProvider);

      //client.register(new ErrorResponseFilter());

      ResteasyWebTarget target = (ResteasyWebTarget)client.target(baseUrl);

      return (T)target.proxy(serviceClass);
    }
}
