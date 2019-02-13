package com.godaddy.vps4.sso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.SsoService;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.SsoToken;

public class SsoTokenExtractorProviderTest {

    private Config mockConfig = mock(Config.class);
    private HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    private SsoTokenExtractor mockOteSsoExtractor = mock(SsoTokenExtractor.class);

    private SsoTokenExtractorProvider ssoTokenExtractorProvider;
    private String defaultSsoUrl = "https://sso.godaddy.com";
    private String fallbackSsoUrl = "https://sso.ote-godaddy.com";

    @Before
    public void setUp() {
        when(mockConfig.get("sso.url")).thenReturn(defaultSsoUrl);
        when(mockConfig.get("sso.url.ote", null)).thenReturn(null);
        ssoTokenExtractorProvider = new SsoTokenExtractorProvider(mockConfig);
    }

    @Test
    public void testExtractTokenTriesBothHeaderAndCookie() throws Exception {
        SsoTokenExtractor extractor = ssoTokenExtractorProvider.get();
        extractor.extractToken(mockRequest);

        verify(mockRequest, times(1)).getHeader("Authorization");
        verify(mockRequest, times(1)).getCookies();
    }

    @Test
    public void testProvidesFallbackExtractor() {
        when(mockConfig.get("sso.url.ote", null)).thenReturn(fallbackSsoUrl);
        SsoTokenExtractor extractor = ssoTokenExtractorProvider.get();

        assertTrue(extractor instanceof FallbackSsoTokenExtractor);
    }

    @Test
    public void testProvidesNonFallbackExtractor() {
        SsoTokenExtractor extractor = ssoTokenExtractorProvider.get();

        assertFalse(extractor instanceof FallbackSsoTokenExtractor);
    }

    @Test
    public void testGetSsoTimeoutMsWithConfig() {
        when(mockConfig.get("auth.timeout.seconds", null)).thenReturn("8600");

        long expectedTimeout = Duration.ofSeconds(8600).toMillis();
        assertEquals(expectedTimeout, ssoTokenExtractorProvider.getSsoTimeoutMs());
    }

    @Test
    public void testGetSsoTimeoutMsDefault() {
        when(mockConfig.get("auth.timeout.seconds", null)).thenReturn(null);

        long expectedTimeout = Duration.ofHours(24).toMillis();
        assertEquals(expectedTimeout, ssoTokenExtractorProvider.getSsoTimeoutMs());
    }

    private SsoTokenExtractorProvider getPatchedSsoTokenExtractorProvider() {
        return new SsoTokenExtractorProvider(mockConfig) {
            @Override
            SsoTokenExtractor getSsoTokenExtractor(SsoService ssoService, long ssTimeoutMs) {
                return mockOteSsoExtractor;
            }
        };
    }

    @Test
    public void testFallbackExtractorFirstTriesDefaultSso() {
        when(mockConfig.get("sso.url.ote", null)).thenReturn(fallbackSsoUrl);
        SsoTokenExtractorProvider patchedProvider = getPatchedSsoTokenExtractorProvider();
        // Using spy since cannot mock a super method call
        SsoTokenExtractor spyExtractor = spy(patchedProvider.get());

        SsoToken mockSsoToken = mock(SsoToken.class);
        doReturn(mockSsoToken).when(spyExtractor).extractAuthorizationHeaderToken(mockRequest);

        SsoToken ssoToken = spyExtractor.extractToken(mockRequest);
        verify(spyExtractor, times(1)).extractAuthorizationHeaderToken(mockRequest);
        verify(mockOteSsoExtractor, never()).extractToken(mockRequest);
        assertEquals(mockSsoToken, ssoToken);
    }

    @Test
    public void testFallbackExtractorTriesFallbackOnException() {
        when(mockConfig.get("sso.url.ote", null)).thenReturn(fallbackSsoUrl);
        SsoTokenExtractorProvider patchedProvider = getPatchedSsoTokenExtractorProvider();
        SsoTokenExtractor spyExtractor = spy(patchedProvider.get());

        // Trick to force super.extractToken in FallbackExtractor to throw exception
        doThrow(new RuntimeException("SSO token signed by unknown key"))
            .when(spyExtractor).extractAuthorizationHeaderToken(mockRequest);

        SsoToken mockSsoToken = mock(SsoToken.class);
        when(mockOteSsoExtractor.extractToken(mockRequest)).thenReturn(mockSsoToken);

        SsoToken ssoToken = spyExtractor.extractToken(mockRequest);
        verify(spyExtractor, times(1)).extractAuthorizationHeaderToken(mockRequest);
        verify(mockOteSsoExtractor, times(1)).extractToken(mockRequest);
        assertEquals(mockSsoToken, ssoToken);
    }

    @Test(expected = RuntimeException.class)
    public void testFallbackExtractorExceptionOnFallback() {
        when(mockConfig.get("sso.url.ote", null)).thenReturn(fallbackSsoUrl);
        SsoTokenExtractorProvider patchedProvider = getPatchedSsoTokenExtractorProvider();
        SsoTokenExtractor spyExtractor = spy(patchedProvider.get());

        // Trick to force super.extractToken in FallbackExtractor to throw exception
        doThrow(new RuntimeException("SSO token signed by unknown key"))
            .when(spyExtractor).extractAuthorizationHeaderToken(mockRequest);
        when(mockOteSsoExtractor.extractToken(mockRequest))
            .thenThrow(new RuntimeException("SSO token signed by unknown key"));

        spyExtractor.extractToken(mockRequest);
    }

}
