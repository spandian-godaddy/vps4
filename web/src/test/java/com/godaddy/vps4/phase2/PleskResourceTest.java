package com.godaddy.vps4.phase2;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.plesk.Vps4PleskService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.util.PollerTimedOutException;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.controlPanel.plesk.PleskResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.HttpHeaders;
import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.mock;


public class PleskResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    private Vps4PleskService pleskServ;

    private GDUser user;
    private VirtualMachine vm;
    private VirtualMachine winVm;

    Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    pleskServ = Mockito.mock(Vps4PleskService.class);
                    bind(Vps4PleskService.class).toInstance(pleskServ);
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));
                    bind(MailRelayService.class).toInstance(mock(MailRelayService.class));
                }

                @Provides
                GDUser provideUser() {
                    return user;
                }
            });

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        vm = createTestVm("hfs-windows-2012r2-plesk-17");
        winVm = createTestVm("hfs-windows-2012r2");

    }

    private VirtualMachine createTestVm(String imageName) {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1", UUID.randomUUID());
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource, imageName);
        return vm;
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private PleskResource getPleskResource() {
        return injector.getInstance(PleskResource.class);
    }

    // === pleskSessionUrl Tests ===
    @Test
    public void testShopperGetPleskSession() throws Exception {
        getPleskResource().getPleskSessionUrl(vm.vmId, "1.2.3.4", null, null);
        Mockito.verify(pleskServ).getPleskSsoUrl(vm.hfsVmId, "1.2.3.4");
    }

    @Test(expected=ForbiddenException.class)
    public void testUnauthorizedShopperGetPleskSession(){
        user = GDUserMock.createShopper("shopperX");
        getPleskResource().getPleskSessionUrl(vm.vmId, "1.2.3.4", null, null);
    }

    public void testAdminGetPleskSession() {
        user = GDUserMock.createAdmin();
        getPleskResource().getPleskSessionUrl(vm.vmId, "1.2.3.4", null, null);
    }

    @Test
    public void testGetPleskSessionInvalidImage() {
        try {
            getPleskResource().getPleskSessionUrl(winVm.vmId, "1.2.3.4", null, null);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testGetPleskSessionXForwardedFor() throws Exception {
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        Mockito.when(headers.getRequestHeader("X-Forwarded-For")).thenReturn(
                Arrays.asList("2.3.4.5, 3.4.5.6"));

        getPleskResource().getPleskSessionUrl(vm.vmId, "", headers, null);
        Mockito.verify(pleskServ).getPleskSsoUrl(vm.hfsVmId, "2.3.4.5");
    }

    @Test
    public void testGetPleskSessionRemoteAddr() throws Exception {
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRemoteAddr()).thenReturn("4.5.6.7");

        getPleskResource().getPleskSessionUrl(vm.vmId, null, headers, req);
        Mockito.verify(pleskServ).getPleskSsoUrl(vm.hfsVmId, "4.5.6.7");
    }

    @Test(expected=NotAcceptableException.class)
    public void testGetPleskSessionNotAcceptable() {
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);

        getPleskResource().getPleskSessionUrl(vm.vmId, "", headers, req);
    }

    @Test
    public void testGetPleskSessionIgnoresPleskServiceException() throws Exception {
        Mockito.when(pleskServ.getPleskSsoUrl(Mockito.anyLong(), Mockito.anyString()))
                .thenThrow(new PollerTimedOutException("Timed out"));
        Assert.assertNull(getPleskResource().getPleskSessionUrl(vm.vmId, "1.2.3.4", null, null));
    }

    // === pleskAccounts Tests ===
    @Test
    public void testShopperGetPleskAccounts() {
        getPleskResource().listPleskAccounts(vm.vmId);
    }

    @Test(expected=ForbiddenException.class)
    public void testUnauthorizedShopperGetPleskAccounts() {
        user = GDUserMock.createShopper("shopperX");
        getPleskResource().listPleskAccounts(vm.vmId);
    }

    @Test
    public void testAdminGetPleskAccounts() {
        user = GDUserMock.createAdmin();
        getPleskResource().listPleskAccounts(vm.vmId);
    }

    @Test
    public void testGetPleskAccountsInvalidImage() {
        try {
            getPleskResource().listPleskAccounts(winVm.vmId);
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_IMAGE", e.getId());
        }
    }

    @Test
    public void testGetPleskAccountsIgnoresPleskServiceException() throws Exception {
        Mockito.when(pleskServ.listPleskAccounts(Mockito.anyLong()))
                .thenThrow(new PollerTimedOutException("Timed out"));
        Assert.assertNull(getPleskResource().listPleskAccounts(vm.vmId));
    }

    @Test
    public void testGetPleskAccountsSuspended() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            getPleskResource().listPleskAccounts(winVm.vmId);
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }
}
