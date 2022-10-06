package com.godaddy.vps4.customer;

import java.util.UUID;

import javax.cache.Cache;
import javax.cache.CacheManager;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.clients.Vps4SsoCustomerApi;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
import com.google.inject.Inject;

public class DefaultCustomerService implements CustomerService {
    private static final String CACHE_KEY = "customer-api";

    private final Cache<String, String> cache;
    private final CustomerApiService customerApiService;
    private final Vps4SsoService vps4SsoService;

    @Inject
    public DefaultCustomerService(CacheManager cacheManager,
                                  CustomerApiService customerApiService,
                                  @Vps4SsoCustomerApi Vps4SsoService vps4SsoService) {
        this.cache = cacheManager.getCache(CacheName.API_ACCESS_TOKENS, String.class, String.class);
        this.customerApiService = customerApiService;
        this.vps4SsoService = vps4SsoService;
    }

    private String getAuthHeader() {
        if (cache.containsKey(CACHE_KEY)) {
            return "sso-jwt " + cache.get(CACHE_KEY);
        }
        Vps4SsoToken token = vps4SsoService.getToken("cert");
        cache.put(CACHE_KEY, token.value());
        return "sso-jwt " + token.value();
    }

    @Override
    public Customer getCustomer(UUID customerId) {
        return customerApiService.getCustomer(getAuthHeader(), customerId);
    }
}
