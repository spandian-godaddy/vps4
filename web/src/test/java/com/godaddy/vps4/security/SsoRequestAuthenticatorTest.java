package com.godaddy.vps4.security;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.SsoRequestAuthenticator;
import com.godaddy.vps4.web.util.AlphaHelper;

import junit.framework.Assert;

public class SsoRequestAuthenticatorTest {

    SsoTokenExtractor tokenExtractor = Mockito.mock(SsoTokenExtractor.class);
    AlphaHelper alphaHelper = Mockito.mock(AlphaHelper.class);
    HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    SsoRequestAuthenticator authenticator = new SsoRequestAuthenticator(tokenExtractor, alphaHelper);

    public SsoToken mockIdpToken(String shopperId) {
        IdpSsoToken token = Mockito.mock(IdpSsoToken.class);
        when(token.getShopperId()).thenReturn(shopperId);
        return token;
    }

    public SsoToken mockJomaxToken(List<String> groups) {
        JomaxSsoToken token = Mockito.mock(JomaxSsoToken.class);
        when(token.getGroups()).thenReturn(groups);
        return token;
    }

    @Test
    public void testAuthenticateInvalidToken() {
        when(tokenExtractor.extractToken(request)).thenReturn(null);

        Assert.assertNull(authenticator.authenticate(request));
    }

    @Test
    public void testAuthenticateShopper() {
        SsoToken token = mockIdpToken("shopperid");
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals("shopperid", user.getShopperId());
        Assert.assertEquals(true, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(false, user.isEmployee());
    }

    @Test
    public void testShopperCannotOverride() {
        when(request.getHeader("X-Shopper-Id")).thenReturn("shopperX");
        SsoToken token = mockIdpToken("shopperid");
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals("shopperid", user.getShopperId());
        Assert.assertEquals(true, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(false, user.isEmployee());
    }

    @Test
    public void testAdmin() {
        SsoToken token = mockJomaxToken(Arrays.asList("Dev-VPS4"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(true, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
    }

    @Test
    public void testAdminWithShopperOverride() {
        when(request.getHeader("X-Shopper-Id")).thenReturn("shopperX");
        SsoToken token = mockJomaxToken(Arrays.asList("Dev-VPS4"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals("shopperX", user.getShopperId());
        Assert.assertEquals(true, user.isShopper());
        Assert.assertEquals(true, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
    }

    @Test
    public void testEmployee() {
        SsoToken token = mockJomaxToken(Arrays.asList("Development"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true,  user.isEmployee());
    }

    @Test
    public void testEmployeeToShopper() {
        SsoToken token = mockIdpToken("shopperid");
        token.employeeUser = mockJomaxToken(Arrays.asList("Development"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals("shopperid", user.getShopperId());
        Assert.assertEquals(true, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true,  user.isEmployee());
    }

    @Test(expected=Vps4Exception.class)
    public void testUnknownSsoTokenType() {
        SsoToken token = Mockito.mock(SsoToken.class);
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        authenticator.authenticate(request);
    }
}
