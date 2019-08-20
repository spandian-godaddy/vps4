package com.godaddy.vps4.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.GDUser.Role;
import com.godaddy.vps4.web.security.XCertSubjectHeaderAuthenticator;
import junit.framework.Assert;
import org.junit.Test;

public class XCertSubjectHeaderAuthenticatorTest {

    HttpServletRequest request = mock(HttpServletRequest.class);
    Config config = mock(Config.class);

    @Test
    public void authenticationOkWhenClientIsScheduler() throws Exception {
        String cn = "FOOBAR";
        when(config.get("vps4.scheduler.certCN")).thenReturn(cn);
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=" + cn + "," +
                    "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        GDUser user = authenticator.authenticate(request);
        Assert.assertNotNull(user);
        Assert.assertEquals(Role.ADMIN, user.role());
        Assert.assertNull(user.getShopperId());
    }

    @Test
    public void authenticationOkWhenClientIsDeveloper() throws Exception {
        String cn = "FOOBAR";
        when(config.get("vps4.developer.certCN")).thenReturn(cn);
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=" + cn + "," +
                "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        GDUser user = authenticator.authenticate(request);
        Assert.assertNotNull(user);
        Assert.assertEquals(Role.ADMIN, user.role());
        Assert.assertNull(user.getShopperId());
    }

    @Test
    public void authenticationOkWhenClientIsMessageConsumer() throws Exception {
        String cn = "FOOBAR";
        when(config.get("vps4.consumer.certCN")).thenReturn(cn);
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=" + cn + "," +
                "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        GDUser user = authenticator.authenticate(request);
        Assert.assertNotNull(user);
        Assert.assertEquals(Role.ADMIN, user.role());
        Assert.assertNull(user.getShopperId());
    }

    @Test
    public void authenticationWithShopperOverride() throws Exception{
        when(config.get("vps4.scheduler.certCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=VPS4 Scheduler Client (MOCK)," +
                "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        when(request.getHeader("X-Shopper-Id")).thenReturn("12345");

        GDUser user = authenticator.authenticate(request);

        Assert.assertNotNull(user);
        Assert.assertEquals(Role.ADMIN, user.role());
        Assert.assertEquals("12345", user.getShopperId());
    }

    @Test
    public void rejectsWithBadCnValue() throws Exception{
        when(config.get("vps4.scheduler.certCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=BAD CN VALUE," +
                "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        GDUser user = authenticator.authenticate(request);

        Assert.assertNull(user);
    }

    @Test
    public void rejectsWithBadHeaderValue() throws Exception{
        when(config.get("vps4.scheduler.certCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("This is a bad header value");

        GDUser user = authenticator.authenticate(request);

        Assert.assertNull(user);
    }

    @Test
    public void rejectsWithNoHeader() throws Exception{
        when(config.get("vps4.scheduler.certCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn(null);

        GDUser user = authenticator.authenticate(request);

        Assert.assertNull(user);
    }
}
