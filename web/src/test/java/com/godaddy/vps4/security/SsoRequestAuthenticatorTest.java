package com.godaddy.vps4.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

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

public class SsoRequestAuthenticatorTest {

    private SsoTokenExtractor tokenExtractor = mock(SsoTokenExtractor.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private Config config = mock(Config.class);
    private String customerId = String.valueOf(UUID.randomUUID());
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
        when(token.getCustomerId()).thenReturn(customerId);
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
        Assert.assertEquals(Arrays.asList(Role.CUSTOMER), user.roles());
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
        SsoToken token = mockJomaxToken(Collections.singletonList("Dev-VPS4"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(true, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.ADMIN), user.roles());
    }

    @Test
    public void testPTGSRole() {
        SsoToken token = mockJomaxToken(Collections.singletonList("Dev-PTGS"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(true, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.ADMIN), user.roles());
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
        Assert.assertEquals(Arrays.asList(Role.HS_LEAD), user.roles());
    }

    @Test
    public void testTechnicalServiceSysAdmin() {
        SsoToken token = mockJomaxToken(Collections.singletonList("org-technical-services-sysadmins"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.HS_LEAD), user.roles());
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
        Assert.assertEquals(Arrays.asList(Role.HS_AGENT), user.roles());
    }


    @Test
    public void testMediaTempleCS() {
        SsoToken token = mockJomaxToken(Collections.singletonList("Media Temple - CS"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.HS_AGENT), user.roles());
    }

    @Test
    public void testMultiSSOGroups() {
        Vector<String> ssoGroups = new Vector<>();
        ssoGroups.add("C3-Hosting Support");
        ssoGroups.add("DCU-Phishstory");

        SsoToken token = mockJomaxToken(Collections.list(Collections.enumeration(ssoGroups)));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        //expect suspend_auth to take precedence over hs_agent role
        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.SUSPEND_AUTH, Role.HS_AGENT), user.roles());
    }

    @Test
    public void testLegalRep() {
        SsoToken token = mockJomaxToken(Collections.singletonList("fs-Legal_IP_Claims"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.SUSPEND_AUTH), user.roles());
    }

    @Test
    public void testHostingOps() {
        SsoToken token = mockJomaxToken(Collections.singletonList("Hosting Ops"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.SUSPEND_AUTH), user.roles());
    }

    @Test
    public void testDigitalCrimesUnit() {
        SsoToken token = mockJomaxToken(Collections.singletonList("DCU-Phishstory"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.SUSPEND_AUTH), user.roles());
    }

    @Test
    public void testChargebackRole() {
        SsoToken token = mockJomaxToken(Collections.singletonList("Chargeback User"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.SUSPEND_AUTH), user.roles());
    }

    @Test
    public void testMigrationRole() {
        SsoToken token = mockJomaxToken(Collections.singletonList("Migration-Engine-SG"));
        when(tokenExtractor.extractToken(request)).thenReturn(token);

        GDUser user = authenticator.authenticate(request);
        Assert.assertEquals(null, user.getShopperId());
        Assert.assertEquals(false, user.isShopper());
        Assert.assertEquals(false, user.isAdmin());
        Assert.assertEquals(true, user.isEmployee());
        Assert.assertEquals(Arrays.asList(Role.MIGRATION), user.roles());
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
        Assert.assertEquals(Arrays.asList(Role.ADMIN), user.roles());
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
        Assert.assertEquals(Arrays.asList(Role.EMPLOYEE_OTHER), user.roles());
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
        Assert.assertEquals(Arrays.asList(Role.EMPLOYEE_OTHER), user.roles());
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
