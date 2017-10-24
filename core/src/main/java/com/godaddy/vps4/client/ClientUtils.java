package com.godaddy.vps4.client;

import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

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
        return new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                String shopperId = shopperIdProvider.get();
                if (shopperId != null) {
                    logger.info("************ Shopper id: {} *************", shopperId);
                    requestContext.getHeaders().add("X-Shopper-Id", shopperId);
                }
            }
        };
    }

    public static ClientRequestFilter getSsoJwtInjectionFilter(SsoTokenService ssoTokenService) {
        return new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                String authToken = String.format("sso-jwt %s", ssoTokenService.getJwt());
                requestContext.getHeaders().add("Authorization", authToken);
            }
        };
    }
}
