package com.godaddy.vps4.security.sso;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.vps4.sso.SsoModule;

public class SsoTokenExtractorProviderTest {

    @Test
    public void testCookieAuthenticate() throws Exception {
        SsoModule ssoModule = new SsoModule();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Config config = Mockito.mock(Config.class);
        when(config.get(eq("auth.timeout"), any())).thenReturn("8600");
        when(config.get(eq("sso.url"))).thenReturn("https://sso.dev-godaddy.com");

        SsoTokenExtractor extractor = ssoModule.provideTokenExtractor(config);
        extractor.extractToken(request);

        verify(request, times(1)).getHeader("Authorization");
        verify(request, never()).getCookies();
    }

}
