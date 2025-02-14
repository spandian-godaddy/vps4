package com.godaddy.vps4.phase2;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.jdbc.JdbcDataCenterService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVmUserService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmPatchResource.VmPatch;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.ForbiddenException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VmPatchResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject VirtualMachineService virtualMachineService;
    @Inject CreditService creditService;

    ShopperNotesService shopperNotesService = mock(ShopperNotesService.class);;
    ActionService actionService = mock(ActionService.class);

    private GDUser user;
    private String initialName;
    private VirtualMachine virtualMachine;
    @Captor
    private ArgumentCaptor<Map<ECommCreditService.ProductMetaField, String>> prodMetaArgumentCaptor;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    // Action service
                    Action coreVmAction = new Action(123L, UUID.randomUUID(), ActionType.UPDATE_SERVER,
                            "", "", "", ActionStatus.COMPLETE,
                            Instant.now(), Instant.now(), "", UUID.randomUUID(), "tester");
                    Mockito.when(actionService.getAction(Mockito.anyLong()))
                            .thenReturn(coreVmAction);
                    bind(ActionService.class).toInstance(actionService);
                    bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
                    bind(ImageService.class).toInstance(Mockito.mock(ImageService.class));
                    bind(VmUserService.class).to(JdbcVmUserService.class);
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(DataCenterService.class).to(JdbcDataCenterService.class);
                    bind(PrivilegeService.class).to(JdbcPrivilegeService.class);
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));
                    bind(MailRelayService.class).toInstance(mock(MailRelayService.class));
                    bind(ShopperNotesService.class).toInstance(shopperNotesService);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1", UUID.randomUUID());
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource, 5);
        return vm;
    }

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();

        virtualMachine = createTestVm();
        initialName = virtualMachine.name;
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmPatchResource getVmPatchResource() {
        return injector.getInstance(VmPatchResource.class);
    }

    private void testValidServerName(String newName){
        VirtualMachine vm = updateVmName(newName);
        assertEquals(newName, vm.name);
        verify(actionService, times(1)).completeAction(anyLong(), eq("{}"), eq(newName));
    }

    private VirtualMachine updateVmName(String newName) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(initialName, vm.name);
        VmPatch vmPatch = new VmPatch();
        vmPatch.name = newName;
        getVmPatchResource().updateVm(vm.vmId, vmPatch);
        vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        return vm;
    }

    @Test
    public void testShopperUpdateServerName(){
        testValidServerName("NewVmName");
    }

    @Test(expected=ForbiddenException.class)
    public void testUnauthShopperUpdateName() {
        user = GDUserMock.createShopper("shopperX");
        testValidServerName("NewVmName");
    }

    @Test
    public void testAdminUpdateServerName(){
        user = GDUserMock.createAdmin();
        testValidServerName("NewVmName");
    }

    @Test
    public void testPunctuationAllowed(){
        testValidServerName("PunctuationOkay!@#$%^&*()-=+\"'");
    }

    @Test
    public void testSpacesOkay(){
        testValidServerName("This VM Name Has Spaces");
    }

    @Test
    public void testNonAlphabetical(){
        testValidServerName("º∂å∑¬˚∆´");
    }

    @Test
    public void testSetsEcommCommonName(){
        testValidServerName("NewVmName");
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        verify(creditService, times(1)).setCommonName(vm.orionGuid, "NewVmName");
    }

    @Test
    public void testEmptyName(){
        // When an empty string is passed there is no change.
        VirtualMachine vm = updateVmName(new String(""));
        assertEquals(initialName, vm.name);
    }

    @Test
    public void testNoName() {
        // When name is not passed there is no change.
        VmPatch vmPatch = new VmPatch();
        getVmPatchResource().updateVm(virtualMachine.vmId, vmPatch);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(initialName, vm.name);
    }

    @Test
    public void testUpdateDataCenterId(){
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(5, vm.dataCenter.dataCenterId);
        getVmPatchResource().updateVmDataCenter(vm.vmId);
        vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(3, vm.dataCenter.dataCenterId);
    }

    @Test
    public void testUpdateDataCenterProdMeta(){
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(5, vm.dataCenter.dataCenterId);
        getVmPatchResource().updateVmDataCenter(vm.vmId);
        vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        verify(creditService, times(1)).updateProductMeta(eq(vm.orionGuid), prodMetaArgumentCaptor.capture());
        Map<ECommCreditService.ProductMetaField, String> prodMeta = prodMetaArgumentCaptor.getValue();
        assertEquals(1, prodMeta.size());
        assertEquals("3", prodMeta.get(ECommCreditService.ProductMetaField.DATA_CENTER));
    }

    @Test
    public void testUpdateDataCenterCallsShopperNotes(){
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        String shopperNote = "Server was migrated from India to Singapore data center by " + user.getUsername() +
                ". VM ID: " + vm.vmId +
                ". Credit ID: " + vm.orionGuid + ".";

        getVmPatchResource().updateVmDataCenter(vm.vmId);
        verify(shopperNotesService, times(1)).processShopperMessage(eq(vm.vmId), eq(shopperNote));
    }

    @Test
    public void testUpdateDataCenterIdWrongOriginDataCenter(){
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        getVmPatchResource().updateVmDataCenter(vm.vmId);
        vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(3, vm.dataCenter.dataCenterId);
        try {
            getVmPatchResource().updateVmDataCenter(vm.vmId);
            fail();
        } catch (Vps4Exception e) {
            assertEquals("WRONG_ORIGIN_DC", e.getId());
        }
    }
}
