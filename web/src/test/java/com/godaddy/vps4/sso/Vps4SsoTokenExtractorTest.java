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
import com.godaddy.hfs.sso.token.CertificateToken;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;

public class Vps4SsoTokenExtractorTest {

    HttpServletRequest request = mock(HttpServletRequest.class);
    Cookie idpCookie = mock(Cookie.class);
    Cookie jomaxCookie = mock(Cookie.class);
    Cookie[] cookies = { idpCookie };
    Cookie[] filteredIdpCookies = { idpCookie };

    SsoToken idpToken = mock(IdpSsoToken.class);
    SsoToken jomaxToken = mock(JomaxSsoToken.class);
    CertificateToken certSsoToken = mock(CertificateToken.class);
    SsoService ssoService = mock(SsoService.class);
    long sessionTimeoutMs = 1000;

    Vps4SsoTokenExtractor ssoTokenExtractor;

    @Before
    public void setUp() throws Exception {
        when(request.getCookies()).thenReturn(cookies);
        when(idpCookie.getName()).thenReturn("auth_idp");
        when(idpCookie.getValue()).thenReturn("value");
        when(jomaxCookie.getName()).thenReturn("auth_jomax");
        when(jomaxCookie.getValue()).thenReturn("value");
        ssoTokenExtractor = spy(new Vps4SsoTokenExtractor(ssoService));
        doNothing().when(ssoTokenExtractor).validate(any(SsoToken.class));
    }

    @Test
    public void testRequestWithHeader() {
        doReturn(idpToken).when(ssoTokenExtractor).extractAuthorizationHeaderToken(request);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        verify(ssoTokenExtractor).extractAuthorizationHeaderToken(request);
        verify(ssoTokenExtractor, never()).extractIdpCookie(cookies);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithCertificateSsoHeader() {
        doReturn(certSsoToken).when(ssoTokenExtractor).extractAuthorizationHeaderToken(request);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        verify(ssoTokenExtractor).extractAuthorizationHeaderToken(request);
        verify(ssoTokenExtractor, never()).extractIdpCookie(cookies);
        assertEquals(certSsoToken, token);
    }

    @Test
    public void testRequestWithIdpCookieOnly() throws Exception {
        cookies = new Cookie[1];
        cookies[0] = idpCookie;
        when(request.getCookies()).thenReturn(cookies);
        doReturn(Collections.singletonList(idpToken)).when(ssoTokenExtractor).extractTokens(filteredIdpCookies);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        verify(ssoTokenExtractor).extractIdpCookie(cookies);
        verify(ssoTokenExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithJomaxCookieOnly() throws Exception {
        cookies = new Cookie[1];
        cookies[0] = jomaxCookie;
        when(request.getCookies()).thenReturn(cookies);
        doReturn(Collections.singletonList(jomaxToken)).when(ssoTokenExtractor).extractTokens(filteredIdpCookies);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        verify(ssoTokenExtractor).extractIdpCookie(cookies);
        verify(ssoTokenExtractor, never()).validate(jomaxToken);
        assertNull(token);
    }

    @Test
    public void testRequestWithIdpAndThenJomaxCookies() throws Exception {
        cookies = new Cookie[2];
        cookies[0] = jomaxCookie;
        cookies[1] = idpCookie;

        when(request.getCookies()).thenReturn(cookies);
        doReturn(Arrays.asList(idpToken, jomaxToken)).when(ssoTokenExtractor).extractTokens(filteredIdpCookies);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        verify(ssoTokenExtractor).extractIdpCookie(cookies);
        verify(ssoTokenExtractor).validate(idpToken);
        verify(ssoTokenExtractor, never()).validate(jomaxToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testRequestWithMultipleTokensAndThenIdp() throws Exception {
        cookies = new Cookie[3];
        cookies[0] = jomaxCookie;
        cookies[1] = jomaxCookie;
        cookies[2] = idpCookie;

        when(request.getCookies()).thenReturn(cookies);
        doReturn(Arrays.asList(jomaxToken, jomaxToken, idpToken)).when(ssoTokenExtractor).extractTokens(filteredIdpCookies);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        verify(ssoTokenExtractor, never()).validate(jomaxToken);
        verify(ssoTokenExtractor).extractIdpCookie(cookies);
        verify(ssoTokenExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

    @Test
    public void testInvalidTokens() throws Exception {
        cookies = new Cookie[3];
        cookies[0] = idpCookie;
        cookies[1] = idpCookie;
        cookies[2] = idpCookie;

        SsoToken badToken = mock(IdpSsoToken.class);
        SsoToken expiredToken = mock(IdpSsoToken.class);

        when(request.getCookies()).thenReturn(cookies);
        doReturn(Arrays.asList(badToken, expiredToken, idpToken)).when(ssoTokenExtractor).extractTokens(cookies);
        doThrow(new VerificationException("error")).when(ssoTokenExtractor).validate(badToken);
        doThrow(new TokenExpiredException("expired")).when(ssoTokenExtractor).validate(expiredToken);

        SsoToken token = ssoTokenExtractor.extractToken(request);
        verify(ssoTokenExtractor).validate(badToken);
        verify(ssoTokenExtractor).validate(expiredToken);
        verify(ssoTokenExtractor).validate(idpToken);
        assertEquals(idpToken, token);
    }

}
