package com.godaddy.vps4.client;

import javax.ws.rs.client.ClientRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;

public class ClientUtils {
    private static final Logger logger = LoggerFactory.getLogger(ClientUtils.class);

    public static <T> T withShopperId(String shopperId, WithShopperId<T> clientCall, Class<T> responseClass) {
        ThreadLocalShopperId.set(shopperId);
        try {
            return clientCall.execute();
        }
        finally {
            ThreadLocalShopperId.set(null);
        }
    }

    public static ClientRequestFilter getShopperIdInjectionFilter(Provider<String> shopperIdProvider) {
        return requestContext -> {
            String shopperId = shopperIdProvider.get();
            if (shopperId != null) {
                logger.info("************ Shopper id: {} *************", shopperId);
                requestContext.getHeaders().add("X-Shopper-Id", shopperId);
            }
        };
    }

    public static ClientRequestFilter getSsoJwtInjectionFilter(SsoTokenService ssoTokenService) {
        return requestContext -> {
            String authToken = String.format("sso-jwt %s", ssoTokenService.getJwt());
            requestContext.getHeaders().add("Authorization", authToken);
        };
    }

    public static <T> SsoJwtAuthenticatedServiceProvider<T> getSsoAuthServiceProvider(Class<T> serviceClass,
                                                                                      String baseUrlConfigPropName) {
        return new SsoJwtAuthenticatedServiceProvider<>(baseUrlConfigPropName, serviceClass);
    }

    public static <T> ClientCertAuthenticatedServiceProvider<T> getClientCertAuthServiceProvider(Class<T> serviceClass,
                                                                                                 String baseUrlConfigPropName,
                                                                                                 String clientCertKeyPath,
                                                                                                 String clientCertPath) {
        return new ClientCertAuthenticatedServiceProvider<>(baseUrlConfigPropName, serviceClass, clientCertKeyPath, clientCertPath);
    }
}
