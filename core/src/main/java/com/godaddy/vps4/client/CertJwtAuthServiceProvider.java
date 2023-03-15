package com.godaddy.vps4.client;

import static com.godaddy.vps4.client.ClientUtils.getSsoCertJwtInjectionFilter;

import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.client.ClientRequestFilter;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.sso.CertJwtApi;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CertJwtAuthServiceProvider<T> extends HttpServiceProvider<T> implements Provider<T> {
    private final CertJwtApi certJwtApi;

    @Inject CacheManager cacheManager;
    @Inject Map<CertJwtApi, Vps4SsoService> ssoServiceMap;

    public CertJwtAuthServiceProvider(String baseUrlConfigPropName,
                                      Class<T> serviceClass,
                                      CertJwtApi certJwtApi) {
        super(baseUrlConfigPropName, serviceClass);
        this.certJwtApi = certJwtApi;
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        Cache<CertJwtApi, String> cache = cacheManager.getCache(CacheName.API_ACCESS_TOKENS,
                                                                CertJwtApi.class,
                                                                String.class);
        Vps4SsoService vps4SsoService = ssoServiceMap.get(certJwtApi);
        requestFilters.add(getSsoCertJwtInjectionFilter(cache, vps4SsoService, certJwtApi));
        return requestFilters;
    }
}
