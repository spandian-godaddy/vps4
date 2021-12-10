package com.godaddy.vps4.sso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import java.util.Collections;

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
        verify(ssoTokExtractor, never()).extractIdpCookie(cookies);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithIdpCookieOnly() throws Exception {
        doReturn(Collections.singletonList(idpToken)).when(ssoTokExtractor).extractTokens(cookies);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).extractIdpCookie(cookies);
        verify(ssoTokExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithJomaxCookieOnly() throws Exception {
        doReturn(Collections.singletonList(jomaxToken)).when(ssoTokExtractor).extractTokens(cookies);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).extractIdpCookie(cookies);
        verify(ssoTokExtractor, never()).validate(jomaxToken);
        assertNull(token);
    }

    @Test
    public void testRequestWithIdpAndThenJomaxCookies() throws Exception {
        doReturn(Arrays.asList(idpToken, jomaxToken)).when(ssoTokExtractor).extractTokens(cookies);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor).extractIdpCookie(cookies);
        verify(ssoTokExtractor).validate(idpToken);
        verify(ssoTokExtractor, never()).validate(jomaxToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithMultipleTokensAndThenIdp() throws Exception {
        doReturn(Arrays.asList(jomaxToken, jomaxToken, idpToken)).when(ssoTokExtractor).extractTokens(cookies);

        SsoToken token = ssoTokExtractor.extractToken(request);
        verify(ssoTokExtractor, never()).validate(jomaxToken);
        verify(ssoTokExtractor).extractIdpCookie(cookies);
        verify(ssoTokExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testInvalidTokens() throws Exception {
        SsoToken badToken = mock(IdpSsoToken.class);
        SsoToken expiredToken = mock(IdpSsoToken.class);
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
