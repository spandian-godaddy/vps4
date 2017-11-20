package com.godaddy.vps4.scheduler.security;

import com.godaddy.hfs.config.Config;
import junit.framework.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XCertSubjectHeaderAuthenticatorTest {

    HttpServletRequest request = mock(HttpServletRequest.class);
    Config config = mock(Config.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    @Test
    public void authenticatesWithSchedulerHeader() throws Exception{
        when(config.get("vps4.api.schedulerCertCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=VPS4 Scheduler Client (MOCK)," +
                    "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        Assert.assertTrue(authenticator.authenticate(request));
    }

    @Test
    public void rejectsSchedulerWithBadCnValue() throws Exception{
        when(config.get("vps4.api.schedulerCertCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=BAD CN VALUE," +
                "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        Assert.assertFalse(authenticator.authenticate(request));
    }

    @Test
    public void rejectsSchedulerWithBadHeaderValue() throws Exception{
        when(config.get("vps4.api.schedulerCertCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("This is a bad header value");

        Assert.assertFalse(authenticator.authenticate(request));
    }

    @Test
    public void rejectsSchedulerWithNoHeader() throws Exception{
        when(config.get("vps4.api.schedulerCertCN")).thenReturn("VPS4 Scheduler Client (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn(null);

        Assert.assertFalse(authenticator.authenticate(request));
    }

    @Test
    public void authenticatesWithDeveloperHeader() throws Exception{
        when(config.get("vps4.api.developerCertCN")).thenReturn("VPS4 Web Developer (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=VPS4 Web Developer (MOCK)," +
                "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        Assert.assertTrue(authenticator.authenticate(request));
    }

    @Test
    public void rejectsDeveloperWithBadCnValue() throws Exception{
        when(config.get("vps4.api.developerCertCN")).thenReturn("VPS4 Web Developer (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("CN=BAD CN VALUE," +
                "OU=Hosting Foundation Services,O=GoDaddy.com\\, Inc.,L=Scottsdale,ST=Arizona,C=US");

        Assert.assertFalse(authenticator.authenticate(request));
    }

    @Test
    public void rejectsDeveloperWithBadHeaderValue() throws Exception{
        when(config.get("vps4.api.developerCertCN")).thenReturn("VPS4 Web Developer (MOCK)");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn("This is a bad header value");

        Assert.assertFalse(authenticator.authenticate(request));
    }

    @Test
    public void rejectsDeveloperWithNoHeader() throws Exception{
        when(config.get("vps4.api.developerCertCN")).thenReturn("VPS4 Web Developer (MOCK))");
        XCertSubjectHeaderAuthenticator authenticator = new XCertSubjectHeaderAuthenticator(config);

        when(request.getHeader("X-Cert-Subject-DN")).thenReturn(null);

        Assert.assertFalse(authenticator.authenticate(request));
    }
}
