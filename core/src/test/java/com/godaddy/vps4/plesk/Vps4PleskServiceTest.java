package com.godaddy.vps4.plesk;

import com.godaddy.hfs.plesk.PleskAction;
import com.godaddy.hfs.plesk.PleskAction.Status;
import com.godaddy.hfs.plesk.PleskService;
import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class Vps4PleskServiceTest {
    private Injector injector;

    @Inject NetworkService networkService;
    @Inject PleskService pleskService;
    @Inject Vps4PleskActionPoller poller;

    PleskAction pleskAction;
    String vmIpAddress;
    IpAddress ipAddress;
    final int hfsVmId = 6360;

    Vps4PleskService vps4PleskService;

    private Injector newInjector() {
        return Guice.createInjector(
                new PleskModule(),
                new ConfigModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(NetworkService.class).toInstance(mock(NetworkService.class));
                        bind(PleskService.class).toInstance(mock(PleskService.class));
                        bind(Vps4PleskActionPoller.class).toInstance(mock(Vps4PleskActionPoller.class));

                    }
                }
        );
    }

    @Before
    public void setUp() throws Exception {
        injector = newInjector();
        injector.injectMembers(this);
        configureMockVps4PleskService();
    }

    private void configureMockVps4PleskService() throws Exception {
        vps4PleskService = injector.getInstance(Vps4PleskService.class);

        //setup a fake plesk action;
        pleskAction = new PleskAction();
        pleskAction.actionId = 66;
        pleskAction.serverId = hfsVmId;
        pleskAction.vmId = hfsVmId;
        pleskAction.status = Status.COMPLETE;
        pleskAction.cncRequestId = 46538;
        pleskAction.actionType = 1;
        pleskAction.createdAt = "On-the-day-of-Easter";
        pleskAction.modifiedAt = "On-the-day-of-Pesach";
        pleskAction.completedAt = "On-the-day-of-Gudhi-Padwa";

        //setup a fake IP address
        vmIpAddress = "192.169.148.52";
        ipAddress = new IpAddress();
        ipAddress.ipAddress = vmIpAddress;
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testListPleskAccounts() throws Exception {
        String responsePayload = "{\"subscriptions\":[\"atuls-test-plesk.org\",\"atuls-test-plesk-02.org\"],\"sites\":[{\"name\":\"atuls-test-plesk-02.org\",\"webspace\":\"Physical hosting\",\"ip_address\":\"10.198.32.154\",\"ftp_login\":\"eric\",\"diskused\":\"297 KB\"},{\"name\":\"atuls-test-plesk.org\",\"webspace\":\"Physical hosting\",\"ip_address\":\"10.198.32.154\",\"ftp_login\":\"atulbhoite\",\"diskused\":\"297 KB\"},{\"name\":\"tannersite.org\",\"webspace\":\"Physical hosting\",\"ip_address\":\"10.198.32.154\",\"ftp_login\":\"eric\",\"diskused\":\"0 B\"}]}";

        pleskAction.responsePayload = responsePayload;

        when(pleskService.requestSiteList(hfsVmId)).thenReturn(pleskAction);
        when(poller.poll(eq(pleskAction), anyInt())).thenReturn(responsePayload);

        try {
            List<PleskSubscription> pleskAccounts = vps4PleskService.listPleskAccounts(hfsVmId);
            assertNotNull("Plesk accounts list should not be empty.", pleskAccounts);
            assertEquals("Plesk accounts list length does not match expected.", 2, pleskAccounts.size());
            assertEquals("Expected plesk subscription does not match actual.", "atuls-test-plesk.org", pleskAccounts.get(0).getName());
            assertEquals("Expected plesk subscription does not match actual.", "atuls-test-plesk-02.org", pleskAccounts.get(1).getName());
        } catch (Exception e) {
            fail("Encountered unexpected exception in listPleskAccounts, failing test. Exception: " + e);
        }
    }

    @Test
    public void testGetPleskSsoUrl() throws Exception {
        String fromIpAddress = "my.super.fake.ip.address";
        String responsePayload = "{\"ssoUrl\":\"https://" + vmIpAddress + ":8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04\"}";
        String expectedSsoUrl = "https://" + vmIpAddress + ":8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04";

        pleskAction.responsePayload = responsePayload;

        when(pleskService.requestAccess(hfsVmId, fromIpAddress)).thenReturn(pleskAction);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ipAddress);
        when(poller.poll(eq(pleskAction), anyInt())).thenReturn(responsePayload);

        try {
            PleskSession session = vps4PleskService.getPleskSsoUrl(hfsVmId, fromIpAddress);
            assertEquals("Expected SSO URL does not match actual SSO URL.", expectedSsoUrl, session.getSsoUrl());
        } catch (Exception e) {
            fail("Encountered unexpected exception in getPleskSsoUrl, failing test. Exception: " + e);
        }
    }

    @Test
    public void testServiceReplacesDomainWithIP() throws Exception {
        String fromIpAddress = "my.super.fake.ip.address";
        String responsePayload = "{\"ssoUrl\":\"https://some.fake.hostname:8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04\"}";
        String expectedSsoUrl = "https://" + vmIpAddress + ":8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04";

        pleskAction.responsePayload = responsePayload;

        when(pleskService.requestAccess(hfsVmId, fromIpAddress)).thenReturn(pleskAction);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ipAddress);
        when(poller.poll(eq(pleskAction), anyInt())).thenReturn(responsePayload);

        try {
            PleskSession session = vps4PleskService.getPleskSsoUrl(hfsVmId, fromIpAddress);
            assertEquals("Expected SSO URL does not match actual SSO URL.", expectedSsoUrl, session.getSsoUrl());
        } catch (Exception e) {
            fail("Encountered unexpected exception in getPleskSsoUrl, failing test. Exception: " + e);
        }
    }

    @Test
    public void testServiceReplacesAnotherDomainWithIP() throws Exception {
        String fromIpAddress = "my.super.fake.ip.address";
        String responsePayload = "{\"ssoUrl\":\"https://HOSTNAME.with-no.p0rt/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04\"}";
        String expectedSsoUrl = "https://" + vmIpAddress + "/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04";

        pleskAction.responsePayload = responsePayload;

        when(pleskService.requestAccess(hfsVmId, fromIpAddress)).thenReturn(pleskAction);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ipAddress);
        when(poller.poll(eq(pleskAction), anyInt())).thenReturn(responsePayload);

        try {
            PleskSession session = vps4PleskService.getPleskSsoUrl(hfsVmId, fromIpAddress);
            assertEquals("Expected SSO URL does not match actual SSO URL.", expectedSsoUrl, session.getSsoUrl());
        } catch (Exception e) {
            fail("Encountered unexpected exception in getPleskSsoUrl, failing test. Exception: " + e);
        }
    }

    @Test
    public void testServicePollsOnAction() throws Exception {
        String fromIpAddress = "my.super.fake.ip.address";
        String responsePayload = "{\"ssoUrl\":\"https://" + vmIpAddress + ":8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04\"}";

        pleskAction.responsePayload = responsePayload;

        when(pleskService.requestAccess(hfsVmId, fromIpAddress)).thenReturn(pleskAction);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ipAddress);
        when(poller.poll(eq(pleskAction), anyInt())).thenReturn(responsePayload);

        try {
            vps4PleskService.getPleskSsoUrl(hfsVmId, fromIpAddress);
            verify(poller, times(1)).poll(eq(pleskAction), anyInt());
        } catch (Exception e) {
            fail("Encountered unexpected exception in getPleskSsoUrl, failing test. Exception: " + e);
        }
    }

    @Test
    public void testRequestAccessIsCalled() throws Exception {
        String fromIpAddress = "my.super.fake.ip.address";
        String responsePayload = "{\"ssoUrl\":\"https://" + vmIpAddress + ":8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04\"}";

        pleskAction.responsePayload = responsePayload;

        when(pleskService.requestAccess(hfsVmId, fromIpAddress)).thenReturn(pleskAction);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ipAddress);
        when(poller.poll(eq(pleskAction), anyInt())).thenReturn(responsePayload);

        try {
            vps4PleskService.getPleskSsoUrl(hfsVmId, fromIpAddress);
            verify(pleskService, times(1)).requestAccess(hfsVmId, fromIpAddress);
        } catch (Exception e) {
            fail("Encountered unexpected exception in getPleskSsoUrl, failing test. Exception: " + e);
        }
    }

    @Test
    public void testGetIpIsCalled() throws Exception {
        String fromIpAddress = "my.super.fake.ip.address";
        String responsePayload = "{\"ssoUrl\":\"https://" + vmIpAddress + ":8443/enterprise/rsession_init.php?PLESKSESSID=5fe2a93e3b182912b899be1213d50c04\"}";

        pleskAction.responsePayload = responsePayload;

        when(pleskService.requestAccess(hfsVmId, fromIpAddress)).thenReturn(pleskAction);
        when(networkService.getVmPrimaryAddress(hfsVmId)).thenReturn(ipAddress);
        when(poller.poll(eq(pleskAction), anyInt())).thenReturn(responsePayload);

        try {
            vps4PleskService.getPleskSsoUrl(hfsVmId, fromIpAddress);
            verify(networkService, times(1)).getVmPrimaryAddress(hfsVmId);
        } catch (Exception e) {
            fail("Encountered unexpected exception in getPleskSsoUrl, failing test. Exception: " + e);
        }
    }
}
