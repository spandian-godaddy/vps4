package com.godaddy.vps4.client;

import javax.cache.Cache;
import javax.ws.rs.client.ClientRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.sso.CertJwtApi;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
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

    public static ClientRequestFilter getSsoCertJwtInjectionFilter(Cache<CertJwtApi, String> cache,
                                                                   Vps4SsoService vps4SsoService,
                                                                   CertJwtApi certJwtApi) {
        return requestContext -> {
            String authToken;
            if (cache.containsKey(certJwtApi)) {
                authToken = "sso-jwt " + cache.get(certJwtApi);
            } else {
                Vps4SsoToken token = vps4SsoService.getToken("cert");
                cache.put(certJwtApi, token.value());
                authToken = "sso-jwt " + token.value();
            }
            requestContext.getHeaders().add("Authorization", authToken);
        };
    }

    public static <T> SsoJwtAuthServiceProvider<T> getSsoAuthServiceProvider(Class<T> serviceClass,
                                                                             String baseUrlConfigPropName) {
        return new SsoJwtAuthServiceProvider<>(baseUrlConfigPropName, serviceClass);
    }

    public static <T> ClientCertAuthServiceProvider<T> getClientCertAuthServiceProvider(Class<T> serviceClass,
                                                                                        String baseUrlConfigPropName,
                                                                                        String clientCertKeyPath,
                                                                                        String clientCertPath) {
        return new ClientCertAuthServiceProvider<>(baseUrlConfigPropName, serviceClass, clientCertKeyPath, clientCertPath);
    }

    public static <T> CertJwtAuthServiceProvider<T> getCertJwtAuthServiceProvider(Class<T> serviceClass,
                                                                                  String baseUrlConfigPropName,
                                                                                  CertJwtApi certJwtApi) {
        return new CertJwtAuthServiceProvider<>(baseUrlConfigPropName, serviceClass, certJwtApi);
    }
}
