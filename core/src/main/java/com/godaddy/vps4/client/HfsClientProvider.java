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

/*
 * This class is a direct duplicate of CertJwtAuthServiceProvider but extending ClientCertAuthServiceProvider instead of
 * HttpServiceProvider. This is needed until HFS fully switches to cert-JWT auth. Until then, they still require client
 * certs which this class provides.
 */

public class HfsClientProvider<T> extends ClientCertAuthServiceProvider<T> implements Provider<T> {
    private static final String CLIENT_CERTIFICATE_KEY_PATH = "hfs.client.keyPath";
    private static final String CLIENT_CERTIFICATE_PATH = "hfs.client.certPath";

    private final CertJwtApi certJwtApi;

    @Inject CacheManager cacheManager;
    @Inject Map<CertJwtApi, Vps4SsoService> ssoServiceMap;

    public HfsClientProvider(String baseUrlConfigPropName,
                             Class<T> serviceClass,
                             CertJwtApi certJwtApi) {
        super(baseUrlConfigPropName, serviceClass, CLIENT_CERTIFICATE_KEY_PATH, CLIENT_CERTIFICATE_PATH);
        this.certJwtApi = certJwtApi;
    }

    @Override
    public List<ClientRequestFilter> getRequestFilters() {
        List<ClientRequestFilter> requestFilters = super.getRequestFilters();
        Cache<String, String> cache = cacheManager.getCache(CacheName.API_JWT_TOKENS, String.class, String.class);
        Vps4SsoService vps4SsoService = ssoServiceMap.get(certJwtApi);
        requestFilters.add(getSsoCertJwtInjectionFilter(cache, vps4SsoService, certJwtApi));
        return requestFilters;
    }
}
