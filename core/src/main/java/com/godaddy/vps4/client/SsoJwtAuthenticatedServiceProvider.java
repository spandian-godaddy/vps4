package com.godaddy.vps4.client;

import static com.godaddy.vps4.client.ClientUtils.getShopperIdInjectionFilter;
import static com.godaddy.vps4.client.ClientUtils.getSsoJwtInjectionFilter;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestFilter;

import com.google.inject.Provider;

public class SsoJwtAuthenticatedServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {

    @Inject SsoTokenService ssoTokenService;
    @Inject @ShopperId Provider<String> shopperIdProvider;

    public SsoJwtAuthenticatedServiceProvider(String baseUrlConfigPropName,
                                              Class<T> serviceClass) {
        super(baseUrlConfigPropName, serviceClass);
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
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
