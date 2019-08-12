package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.mailrelay.VmMailRelayResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class MailRelayResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject NetworkService networkService;
    private MailRelayService mailRelayService;

    private GDUser user;
    private VirtualMachine vm;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    mailRelayService = mock(MailRelayService.class);
                    bind(MailRelayService.class).toInstance(mailRelayService);
                    SchedulerWebService swServ = mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));
                }

                @Provides
                protected GDUser provideUser() {
                    return user;
                }
            });

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        vm = createTestVm();
        networkService.createIpAddress(1234, vm.vmId, "127.0.0.1", IpAddressType.PRIMARY);
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmMailRelayResource getMailRelayResource() {
        return injector.getInstance(VmMailRelayResource.class);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    // === getMailRelayUsage Tests ===
    @Test
    public void testShopperGetCurrentMailRelayUsage(){
        getMailRelayResource().getCurrentMailRelayUsage(vm.vmId);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetCurrentMailRelayUsage(){
        user = GDUserMock.createShopper("shopperX");
        getMailRelayResource().getCurrentMailRelayUsage(vm.vmId);
    }

    @Test
    public void testAdminGetCurrentMailRelayUsage(){
        user = GDUserMock.createAdmin();
        getMailRelayResource().getCurrentMailRelayUsage(vm.vmId);
    }

    @Test
    public void testShopperGetMailRelayUsageFailsIfSuspended() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        try {
            getMailRelayResource().getCurrentMailRelayUsage(vm.vmId);
            Assert.fail("Exception not thrown");
        } catch(Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    // === getMailRelayHistory Tests ===
    @Test
    public void testShopperGetMailRelayHistory(){
        getMailRelayResource().getMailRelayHistory(vm.vmId, 0);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetMailRelayHistory(){
        user = GDUserMock.createShopper("shopperX");
        getMailRelayResource().getMailRelayHistory(vm.vmId, 0);
    }

    @Test
    public void testAdminGetMailRelayHistory(){
        user = GDUserMock.createAdmin();
        getMailRelayResource().getMailRelayHistory(vm.vmId, 0);
    }

    @Test
    public void testAdminGetMailRelayHistory0DaysToReturnReturnsAll(){
        user = GDUserMock.createAdmin();
        getMailRelayResource().getMailRelayHistory(vm.vmId, 0);
        LocalDateTime ldt = LocalDateTime.ofInstant(vm.validOn, ZoneOffset.UTC);
        LocalDate startTime = LocalDate.of(ldt.getYear(), ldt.getMonth(), ldt.getDayOfMonth());
        Mockito.verify(mailRelayService, Mockito.times(1)).getMailRelayHistory("127.0.0.1", startTime);
    }

    @Test
    public void testAdminGetMailRelayHistoryDefineDaysToReturn(){
        user = GDUserMock.createAdmin();
        getMailRelayResource().getMailRelayHistory(vm.vmId, 30);
        LocalDateTime ldt = LocalDateTime.ofInstant(vm.validOn, ZoneOffset.UTC);
        LocalDate startTime = LocalDate.of(ldt.getYear(), ldt.getMonth(), ldt.getDayOfMonth());
        Mockito.verify(mailRelayService, Mockito.times(1)).getMailRelayHistory("127.0.0.1", startTime, 30);
    }

    // === updateMailRelayQuota Tests ===
    private VmMailRelayResource.MailRelayQuotaPatch getQuotaPatch(){
        VmMailRelayResource.MailRelayQuotaPatch patch = new VmMailRelayResource.MailRelayQuotaPatch();
        patch.quota = 1234;
        return patch;
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperUpdateMailRelayQuota(){
        user = GDUserMock.createShopper("shopperX");
        getMailRelayResource().updateMailRelayQuota(vm.vmId, getQuotaPatch());
    }

    @Test
    public void testAdminUpdateMailRelayQuota(){
        user = GDUserMock.createAdmin();
        getMailRelayResource().updateMailRelayQuota(vm.vmId, getQuotaPatch());
    }

    @Test
    public void testStaffUpdateMailRelayQuota(){
        user = GDUserMock.createStaff();
        getMailRelayResource().updateMailRelayQuota(vm.vmId, getQuotaPatch());
    }

}
