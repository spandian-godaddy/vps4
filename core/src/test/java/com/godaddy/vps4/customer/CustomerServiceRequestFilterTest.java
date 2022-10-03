package com.godaddy.vps4.customer;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;

@RunWith(MockitoJUnitRunner.class)
public class CustomerServiceRequestFilterTest {
    @Mock private Cache<String, String> cache;
    @Mock private CacheManager cacheManager;
    @Mock private ClientRequestContext context;
    @Mock private MultivaluedMap<String, Object> headers;
    @Mock private Vps4SsoToken token;
    @Mock private Vps4SsoService vps4SsoService;

    private final String key = "customer-api";
    private final String jwt = "test-jwt";

    private CustomerServiceRequestFilter filter;

    @Before
    public void setup() {
        when(cacheManager.getCache(CacheName.API_ACCESS_TOKENS, String.class, String.class)).thenReturn(cache);
        when(cache.containsKey(key)).thenReturn(true);
        when(cache.get(key)).thenReturn(jwt);
        when(context.getHeaders()).thenReturn(headers);
        when(token.value()).thenReturn(jwt);
        when(vps4SsoService.getToken("cert")).thenReturn(token);
        filter = new CustomerServiceRequestFilter(cacheManager, vps4SsoService);
    }

    @Test
    public void getsTokenFromCache() throws IOException {
        filter.filter(context);
        verify(cache).containsKey(key);
        verify(cache, times(1)).get(key);
        verify(cache, never()).put(key, jwt);
        verify(headers, times(1)).add(anyString(), anyObject());
        verify(headers).add("authorization", "sso-jwt " + jwt);
    }

    @Test
    public void getsTokenFromSsoService() throws IOException {
        when(cache.containsKey(key)).thenReturn(false);
        filter.filter(context);
        verify(cache).containsKey(key);
        verify(cache, never()).get(key);
        verify(cache, times(1)).put(key, jwt);
        verify(headers, times(1)).add(anyString(), anyObject());
        verify(headers).add("authorization", "sso-jwt " + jwt);
    }
}
