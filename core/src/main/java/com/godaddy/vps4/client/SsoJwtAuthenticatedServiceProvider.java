package com.godaddy.vps4.client;

import static com.godaddy.vps4.client.ClientUtils.getShopperIdInjectionFilter;
import static com.godaddy.vps4.client.ClientUtils.getSsoJwtInjectionFilter;

import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestFilter;
import java.util.List;

public class SsoJwtAuthenticatedServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    private static final Logger logger = LoggerFactory.getLogger(SsoJwtAuthenticatedServiceProvider.class);
    @Inject SsoTokenService ssoTokenService;
    @Inject @ShopperId Provider<String> shopperIdProvider;

    public SsoJwtAuthenticatedServiceProvider(String baseUrlConfigPropName,
                                              Class<T> serviceClass) {
        super(baseUrlConfigPropName, serviceClass);
    }

    @Override
    List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        requestFilters.add(getSsoJwtInjectionFilter(ssoTokenService));
        requestFilters.add(getShopperIdInjectionFilter(shopperIdProvider));
        return requestFilters;
    }

    @Override
    public T get() {
        return super.get();
    }
}
