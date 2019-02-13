package com.godaddy.vps4.sso;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.sso.SsoService;
import com.godaddy.hfs.sso.TokenExpiredException;
import com.godaddy.hfs.sso.VerificationException;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;

public class Vps4SsoTokenExtractorTest {

    HttpServletRequest request = mock(HttpServletRequest.class);
    Cookie[] cookies = { mock(Cookie.class) };
    SsoToken idpToken = mock(IdpSsoToken.class);
    SsoToken jomaxToken = mock(JomaxSsoToken.class);
    SsoService ssoService = mock(SsoService.class);
    long sessionTimeoutMs = 1000;

    Vps4SsoTokenExtractor ssoTokExtractor;

    @Before
    public void setUp() throws Exception {
        when(request.getCookies()).thenReturn(cookies);
        ssoTokExtractor = spy(new Vps4SsoTokenExtractor(ssoService, sessionTimeoutMs));
        doNothing().when(ssoTokExtractor).validate(any(SsoToken.class));
    }

    @Test
    public void testRequestWithHeader() {
        doReturn(idpToken).when(ssoTokExtractor).extractAuthorizationHeaderToken(request);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).extractAuthorizationHeaderToken(request);
        verify(ssoTokExtractor, never()).extractCookieTokenWithIdpPriority(cookies);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithIdpCookieOnly() throws Exception {
        doReturn(Arrays.asList(idpToken)).when(ssoTokExtractor).extractTokens(cookies);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).extractCookieTokenWithIdpPriority(cookies);
        verify(ssoTokExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithJomaxCookieOnly() throws Exception {
        doReturn(Arrays.asList(jomaxToken)).when(ssoTokExtractor).extractTokens(cookies);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).extractCookieTokenWithIdpPriority(cookies);
        verify(ssoTokExtractor).validate(jomaxToken);
        assertEquals(jomaxToken, token);
    }

    @Test
    public void testRequestWithIdpAndThenJomaxCookies() throws Exception {
        doReturn(Arrays.asList(idpToken, jomaxToken)).when(ssoTokExtractor).extractTokens(cookies);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).extractCookieTokenWithIdpPriority(cookies);
        verify(ssoTokExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithMultipleTokensAndThenIdp() throws Exception {
        doReturn(Arrays.asList(jomaxToken, jomaxToken, idpToken)).when(ssoTokExtractor).extractTokens(cookies);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).extractCookieTokenWithIdpPriority(cookies);
        verify(ssoTokExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testInvalidTokens() throws Exception {
        SsoToken badToken = mock(SsoToken.class);
        SsoToken expiredToken = mock(SsoToken.class);
        doReturn(Arrays.asList(badToken, expiredToken, idpToken)).when(ssoTokExtractor).extractTokens(cookies);
        doThrow(new VerificationException("error")).when(ssoTokExtractor).validate(badToken);
        doThrow(new TokenExpiredException("expired")).when(ssoTokExtractor).validate(expiredToken);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).validate(badToken);
        verify(ssoTokExtractor).validate(expiredToken);
        verify(ssoTokExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

}
