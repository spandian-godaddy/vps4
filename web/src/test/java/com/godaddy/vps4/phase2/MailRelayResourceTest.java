package com.godaddy.vps4.phase2;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.mailrelay.VmMailRelayResource;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class MailRelayResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject NetworkService networkService;
    private MailRelayService mailRelayService;

    private GDUser user;
    private VirtualMachine vm;

    private Vm hfsVm;
    private long hfsVmId = 98765;


    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    mailRelayService = Mockito.mock(MailRelayService.class);
                    bind(MailRelayService.class).toInstance(mailRelayService);

                    CreditService creditService = Mockito.mock(CreditService.class);
                    bind(CreditService.class).toInstance(creditService);

                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                            .thenReturn(commandState);
                    bind(CommandService.class).toInstance(commandService);

                    hfsVm = new Vm();
                    hfsVm.status = "ACTIVE";
                    hfsVm.vmId = hfsVmId;
                    VmService vmService = Mockito.mock(VmService.class);
                    Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);
                    bind(VmService.class).toInstance(vmService);
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
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
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


}
