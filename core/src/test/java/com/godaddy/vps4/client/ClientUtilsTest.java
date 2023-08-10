package com.godaddy.vps4.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.cache.Cache;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.sso.CertJwtApi;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;

@RunWith(MockitoJUnitRunner.class)
public class ClientUtilsTest {
    @Mock private ClientRequestContext context;
    @Mock private Cache<String, String> cache;
    @Mock private Vps4SsoService vps4SsoService;
    @Mock private MultivaluedMap<String,Object> headers;
    @Mock private Vps4SsoToken vps4SsoToken;

    private final CertJwtApi certJwtApi = CertJwtApi.MESSAGING_API;
    private final String jwt = "test-token";

    @Before
    public void setUp() {
        when(context.getHeaders()).thenReturn(headers);
        when(cache.containsKey(certJwtApi.name())).thenReturn(true);
        when(cache.get(certJwtApi.name())).thenReturn(jwt);
        when(vps4SsoService.getToken("cert")).thenReturn(vps4SsoToken);
        when(vps4SsoToken.value()).thenReturn(jwt);
    }

    @Test
    public void testCertJwtFilterCallsCache() throws IOException {
        ClientRequestFilter crf = ClientUtils.getSsoCertJwtInjectionFilter(cache, vps4SsoService, certJwtApi);
        crf.filter(context);
        verify(cache).containsKey(certJwtApi.name());
        verify(cache).get(certJwtApi.name());
        verify(vps4SsoService, never()).getToken(anyString());
        verify(cache, never()).put(anyString(), anyString());
        verify(headers).add("Authorization", "sso-jwt " + jwt);
    }

    @Test
    public void testCertJwtFilterCallsSsoService() throws IOException {
        when(cache.containsKey(certJwtApi.name())).thenReturn(false);
        ClientRequestFilter crf = ClientUtils.getSsoCertJwtInjectionFilter(cache, vps4SsoService, certJwtApi);
        crf.filter(context);
        verify(cache).containsKey(certJwtApi.name());
        verify(cache, never()).get(certJwtApi.name());
        verify(vps4SsoService).getToken("cert");
        verify(cache).put(certJwtApi.name(), jwt);
        verify(headers).add("Authorization", "sso-jwt " + jwt);
    }
}
