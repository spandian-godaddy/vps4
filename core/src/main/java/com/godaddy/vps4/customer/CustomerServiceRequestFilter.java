package com.godaddy.vps4.customer;

import java.io.IOException;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.clients.Vps4SsoCustomerApi;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
import com.google.inject.Inject;

public class CustomerServiceRequestFilter implements ClientRequestFilter {
    private static final String CACHE_KEY = "customer-api";

    private final CacheManager cacheManager;
    private final Vps4SsoService vps4SsoService;

    @Inject
    public CustomerServiceRequestFilter(CacheManager cacheManager,
                                        @Vps4SsoCustomerApi Vps4SsoService vps4SsoService) {
        this.cacheManager = cacheManager;
        this.vps4SsoService = vps4SsoService;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("authorization", "sso-jwt " + getJwt());
    }

    private String getJwt() {
        Cache<String, String> cache = cacheManager.getCache(CacheName.API_ACCESS_TOKENS, String.class, String.class);
        if (cache.containsKey(CACHE_KEY)) {
            return cache.get(CACHE_KEY);
        }
        Vps4SsoToken token = vps4SsoService.getToken("cert");
        cache.put(CACHE_KEY, token.value());
        return token.value();
    }
}
