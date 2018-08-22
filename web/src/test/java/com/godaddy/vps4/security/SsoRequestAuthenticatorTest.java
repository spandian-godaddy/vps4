package com.godaddy.vps4.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.sso.SsoTokenExtractor;
import com.godaddy.hfs.sso.token.IdpSsoToken;
import com.godaddy.hfs.sso.token.JomaxSsoToken;
import com.godaddy.hfs.sso.token.SsoToken;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.GDUser.Role;
import com.godaddy.vps4.web.security.SsoRequestAuthenticator;
import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.Assert;
import org.junit.Test;

public class SsoRequestAuthenticatorTest {

    private SsoTokenExtractor tokenExtractor = mock(SsoTokenExtractor.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private Config config = mock(Config.class);

    private final Injector injector;
    {
        injector = Guice.createInjector(
                binder -> {
                    binder.bind(Config.class).toInstance(config);
                    binder.bind(SsoTokenExtractor.class).toInstance(tokenExtractor);
                    binder.bind(HttpServletRequest.class).toInstance(request);
                }
        );
    }

    private SsoRequestAuthenticator authenticator = injector.getInstance(SsoRequestAuthenticator.class);

    private SsoToken mockIdpToken(String shopperId) {
        IdpSsoToken token = mock(IdpSsoToken.class);
        when(token.getShopperId()).thenReturn(shopperId);
        return token;
    }

    private SsoToken mockJomaxToken(List<String> groups) {
        JomaxSsoToken token = mock(JomaxSsoToken.class);
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
        Assert.assertEquals(false, user.isStaff());
        Assert.assertEquals(Role.CUSTOMER, user.role());
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
        Assert.assertEquals(false, user.isStaff());
    }

    @Test
    public void testAdmin() {
        SsoToken token = mockJomaxToken(Collections.singletonList("Dev-VPS4"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(true, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(true, user.isStaff());
        Assert.assertEquals(Role.ADMIN, user.role());
    }

    @Test
    public void testHostingSupportLead() {
        SsoToken token = mockJomaxToken(Collections.singletonList("HS_techleads"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(true, user.isStaff());
        Assert.assertEquals(Role.HS_LEAD, user.role());
    }

    @Test
    public void testHostingSupportAgent() {
        SsoToken token = mockJomaxToken(Collections.singletonList("C3-Hosting Support"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(true, user.isStaff());
        Assert.assertEquals(Role.HS_AGENT, user.role());
    }

    @Test
    public void testAdminWithShopperOverride() {
        when(request.getHeader("X-Shopper-Id")).thenReturn("shopperX");
        SsoToken token = mockJomaxToken(Collections.singletonList("Dev-VPS4"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals("shopperX", user.getShopperId());
        Assert.assertEquals(true, user.isShopper());
        Assert.assertEquals(true, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Role.ADMIN, user.role());
    }

    @Test
    public void testEmployee() {
        SsoToken token = mockJomaxToken(Collections.singletonList("Development"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true,  user.isEmployee());
        Assert.assertEquals(false, user.isStaff());
        Assert.assertEquals(Role.EMPLOYEE_OTHER, user.role());
    }

    @Test
    public void testEmployeeToShopper() {
        SsoToken token = mockIdpToken("shopperid");
        token.employeeUser = mockJomaxToken(Collections.singletonList("Development"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals("shopperid", user.getShopperId());
        Assert.assertEquals(true, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true,  user.isEmployee());
        Assert.assertEquals(false, user.isStaff());
        Assert.assertEquals(Role.EMPLOYEE_OTHER, user.role());
    }

    @Test(expected=Vps4Exception.class)
    public void testUnknownSsoTokenType() {
        SsoToken token = mock(SsoToken.class);
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        authenticator.authenticate(request);
    }

    @Test
    public void test3LetterAccountAllowedAccessToInactiveDC() {
        SsoToken token = mockIdpToken("fak");
        token.employeeUser = mockJomaxToken(Collections.singletonList("Development"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);
        when(config.get("vps4.is.dc.inactive")).thenReturn("true");

        GDUser user = authenticator.authenticate(request);
        Assert.assertNotNull(user);
        Assert.assertEquals("fak", user.getShopperId());
        Assert.assertEquals(true, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true,  user.isEmployee());

    }

    @Test
    public void testShopperDeniedAccessToInactiveDC() {
    SsoToken token = mockIdpToken("random-shopperid");
        when(tokenExtractor.extractToken(request)).thenReturn(token);
        when(config.get("vps4.is.dc.inactive")).thenReturn("true");

        GDUser user = authenticator.authenticate(request);
        Assert.assertNull(user);

    }

    @Test
    public void testShopperAllowedAccessToActiveDC() {
        SsoToken token = mockIdpToken("another-random-shopperid");
        when(tokenExtractor.extractToken(request)).thenReturn(token);
        when(config.get("vps4.is.dc.inactive")).thenReturn("false");

        GDUser user = authenticator.authenticate(request);
        Assert.assertNotNull(user);
        Assert.assertEquals("another-random-shopperid", user.getShopperId());
        Assert.assertEquals(true, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(false,  user.isEmployee());

    }
}
