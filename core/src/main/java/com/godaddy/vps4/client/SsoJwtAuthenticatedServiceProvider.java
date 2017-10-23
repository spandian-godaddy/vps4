package com.godaddy.vps4.client;

import com.godaddy.vps4.util.KeyManagerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.net.ssl.KeyManager;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.List;

public class SsoJwtAuthenticatedServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    private static final Logger logger = LoggerFactory.getLogger(SsoJwtAuthenticatedServiceProvider.class);
    @Inject SsoTokenService ssoTokenService;

    public SsoJwtAuthenticatedServiceProvider(String baseUrlConfigPropName,
                                              Class<T> serviceClass) {
        super(baseUrlConfigPropName, serviceClass);
    }

    @Override
    List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        // Request filter for adding SSO jwt to the auth header
        requestFilters.add(new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                String authToken = String.format("sso-jwt %s", ssoTokenService.getJwt());
                requestContext.getHeaders().add("Authorization", authToken);
            }
        });
        return requestFilters;
    }

    @Override
    public T get() {
        return super.get();
    }
}
