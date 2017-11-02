package com.godaddy.vps4.security;

import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.XCertSubjectHeaderAuthenticator;
import com.godaddy.vps4.web.util.AlphaHelper;
import junit.framework.Assert;
import org.junit.Test;

import com.godaddy.hfs.config.Config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XCertSubjectHeaderAuthenticatorTest {

    AlphaHelper alphaHelper = mock(AlphaHelper.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    Config config = mock(Config.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    public void authenticatesWithHeader() throws Exception{
        when(config.get("vps4.scheduler.certCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config, alphaHelper);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=VPS4 Scheduler Client (MOCK)," +
                    "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        when(request.getHeader("X-Shopper-Id")).thenReturn("12345");

        GDUser user = authenticator.authenticate(request);

        Assert.assertNotNull(user);
    }

    @Test
    public void rejectsWithBadCnValue() throws Exception{
        when(config.get("vps4.scheduler.certCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config, alphaHelper);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=BAD CN VALUE," +
                "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        GDUser user = authenticator.authenticate(request);

        Assert.assertNull(user);
    }

    @Test
    public void rejectsWithBadHeaderValue() throws Exception{
        when(config.get("vps4.scheduler.certCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config, alphaHelper);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("This is a bad header value");

        GDUser user = authenticator.authenticate(request);

        Assert.assertNull(user);
    }

    @Test
    public void rejectsWithNoHeader() throws Exception{
        when(config.get("vps4.scheduler.certCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config, alphaHelper);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn(null);

        GDUser user = authenticator.authenticate(request);

        Assert.assertNull(user);
    }
}
