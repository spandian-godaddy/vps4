package com.godaddy.vps4.phase2;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VirtualMachineDetails;
import com.godaddy.vps4.web.vm.VirtualMachineWithDetails;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

public class VmResourceUserTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private GDUser user;
    private CreditService creditService = Mockito.mock(CreditService.class);
    private Vm hfsVm;
    private long hfsVmId = 98765;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(CreditService.class).toInstance(creditService);

                    // HFS services
                    hfsVm = new Vm();
                    hfsVm.status = "ACTIVE";
                    hfsVm.vmId = hfsVmId;
                    VmService vmService = Mockito.mock(VmService.class);
                    Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);
                    bind(VmService.class).toInstance(vmService);

                    // Command Service
                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                            .thenReturn(commandState);
                    bind(CommandService.class).toInstance(commandService);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });


    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmResource getVmResource() {
        return injector.getInstance(VmResource.class);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    // === GetVm Tests ===
    @Test
    public void testShopperGetVm() {
        VirtualMachine vm = createTestVm();
        UUID expectedGuid = vm.orionGuid;

        user = GDUserMock.createShopper();
        vm = getVmResource().getVm(vm.vmId);
        Assert.assertEquals(expectedGuid, vm.orionGuid);
    }

    @Test(expected = AuthorizationException.class)
    public void testUnauthorizedShopperGetVm() {
        VirtualMachine vm = createTestVm();

        user = GDUserMock.createShopper("shopperX");
        getVmResource().getVm(vm.vmId);
    }

    @Test(expected = NotFoundException.class)
    public void testNoVmGetVm() {
        user = GDUserMock.createShopper();
        UUID noSuchVmId = UUID.randomUUID();

        getVmResource().getVm(noSuchVmId);
    }

    @Test(expected = NotFoundException.class)
    public void testNoLongerValidGetVm() {
        VirtualMachine vm = createTestVm();
        SqlTestData.invalidateTestVm(vm.vmId, dataSource);

        user = GDUserMock.createShopper();
        getVmResource().getVm(vm.vmId);
    }

    @Test
    public void testEmployeeGetVm() {
        VirtualMachine vm = createTestVm();
        UUID expectedGuid = vm.orionGuid;

        user = GDUserMock.createEmployee();
        vm = getVmResource().getVm(vm.vmId);
        Assert.assertEquals(expectedGuid, vm.orionGuid);
    }

    @Test
    public void testAdminGetVm() {
        VirtualMachine vm = createTestVm();
        UUID expectedGuid = vm.orionGuid;

        user = GDUserMock.createAdmin();
        vm = getVmResource().getVm(vm.vmId);
        Assert.assertEquals(expectedGuid, vm.orionGuid);
    }

    // === startVm Tests ===
    public void startVm() {
        VirtualMachine vm = createTestVm();

        Action action = getVmResource().startVm(vm.vmId);
        Assert.assertNotNull(action.commandId);
    }

    @Test
    public void testShopperStartVm() {
        hfsVm.status = "STOPPED";
        startVm();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperStartVm() {
        user = GDUserMock.createShopper("shopperX");
        hfsVm.status = "STOPPED";
        startVm();
    }

    @Test
    public void testAdminStartVm() {
        user = GDUserMock.createAdmin();
        hfsVm.status = "STOPPED";
        startVm();
    }

    @Test
    public void testStartActiveVm() {
        try {
            startVm();
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_STATUS", e.getId());
        }
    }

    @Test
    public void testDoubleStartVm() {
        hfsVm.status = "STOPPED";
        VirtualMachine vm = createTestVm();
        getVmResource().startVm(vm.vmId);
        try {
            getVmResource().startVm(vm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    // === stopVm Tests ===
    public void testStopVm() {
        VirtualMachine vm = createTestVm();

        Action action = getVmResource().stopVm(vm.vmId);
        Assert.assertNotNull(action.commandId);
    }

    @Test
    public void testShopperStopVm() {
        testStopVm();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperStopVm() {
        user = GDUserMock.createShopper("shopperX");
        testStopVm();
    }

    @Test
    public void testEmployeeStopVm() {
        user = GDUserMock.createEmployee();
        testStopVm();
    }

    @Test
    public void testStopInactiveVm() {
        hfsVm.status = "STOPPED";
        try {
            testStopVm();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_STATUS", e.getId());
        }
    }

    @Test
    public void testDoubleStopVm() {
        VirtualMachine vm = createTestVm();
        getVmResource().stopVm(vm.vmId);
        try {
            getVmResource().stopVm(vm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    // === restartVm Tests ===
    public void testRestartVm() {
        VirtualMachine vm = createTestVm();

        Action action = getVmResource().restartVm(vm.vmId);
        Assert.assertNotNull(action.commandId);
    }

    @Test
    public void testShopperRestartVm() {
        testRestartVm();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperRestartVm() {
        user = GDUserMock.createShopper("shopperX");
        testRestartVm();
    }

    @Test
    public void testAdminRestartVm() {
        user = GDUserMock.createAdmin();
        testRestartVm();
    }

    @Test
    public void testE2SRestartVm() {
        user = GDUserMock.createEmployee2Shopper();
        testRestartVm();
    }

    @Test
    public void testRestartInactiveVm() {
        hfsVm.status = "STOPPED";
        try {
            testRestartVm();
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_STATUS", e.getId());
        }
    }

    @Test
    public void testDoubleRestartVm() {
        VirtualMachine vm = createTestVm();
        getVmResource().restartVm(vm.vmId);
        try {
            getVmResource().restartVm(vm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void testStopWhileRestartingVm() {
        VirtualMachine vm = createTestVm();

        Action action = getVmResource().restartVm(vm.vmId);
        Assert.assertNotNull(action.commandId);
        try {
            getVmResource().stopVm(vm.vmId);
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            System.out.println(e.getId());
            Assert.assertNotNull(action.commandId);
        }
    }

    // === destroyVm Tests ===
    public void testDestroyVm() {
        VirtualMachine vm = createTestVm();

        Action action = getVmResource().destroyVm(vm.vmId);
        Assert.assertNotNull(action.commandId);
    }

    @Test
    public void testShopperDestroyVm() {
        testDestroyVm();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperDestroyVm() {
        user = GDUserMock.createShopper("shopperX");
        testDestroyVm();
    }

    @Test
    public void testAdminDestroyVm() {
        user = GDUserMock.createAdmin();
        testDestroyVm();
    }

    @Test
    public void testE2SDestroyVm() {
        user = GDUserMock.createEmployee2Shopper();
        testDestroyVm();
    }

    // === getVMs Tests ===
    @Test
    public void testShopperGetVirtualMachines() {
        VirtualMachine vm = createTestVm();

        user = GDUserMock.createShopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines();
        Assert.assertEquals(1, vms.size());
        Assert.assertEquals(vms.get(0).orionGuid, vm.orionGuid);
    }

    @Test
    public void testShopperGetVirtualMachinesEmpty() {
        user = GDUserMock.createShopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines();
        Assert.assertTrue(vms.isEmpty());
    }

    @Test
    public void testUnauthorizedShopperGetVirtualMachines() {
        createTestVm();

        user = GDUserMock.createShopper("shopperX");
        List<VirtualMachine> vms = getVmResource().getVirtualMachines();
        Assert.assertTrue(vms.isEmpty());
    }

    @Test(expected=Vps4NoShopperException.class)
    public void testAdminFailsGetVirtualMachines() throws InterruptedException {
        createTestVm();

        user = GDUserMock.createAdmin();
        getVmResource().getVirtualMachines();
    }

    @Test
    public void testE2SGetVirtualMachines() {
        VirtualMachine vm = createTestVm();

        user = GDUserMock.createEmployee2Shopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines();
        Assert.assertEquals(1, vms.size());
        Assert.assertEquals(vms.get(0).orionGuid, vm.orionGuid);
    }

    // === getVmDetails Tests ===
    public void testGetVmDetails() {
        VirtualMachine vm = createTestVm();

        VirtualMachineDetails details = getVmResource().getVirtualMachineDetails(vm.vmId);
        Assert.assertEquals(details.status, "ACTIVE");
        Assert.assertEquals(details.vmId.longValue(), hfsVmId);
    }

    @Test
    public void testShopperGetVmDetails() {
        testGetVmDetails();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetVmDetails() {
        user = GDUserMock.createShopper("shopperX");
        testGetVmDetails();
    }

    @Test
    public void testAdminGetVmDetails() {
        user = GDUserMock.createAdmin();
        testGetVmDetails();
    }

    // === getVmWithDetails Tests ===
    public void testGetVmWithDetails() {
        VirtualMachine vm = createTestVm();
        VirtualMachineCredit credit = Mockito.mock(VirtualMachineCredit.class);
        Mockito.when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);

        VirtualMachineWithDetails detailedVm = getVmResource().getVirtualMachineWithDetails(vm.vmId);
        Assert.assertEquals(detailedVm.orionGuid, vm.orionGuid);
        Assert.assertEquals(detailedVm.virtualMachineDetails.vmId.longValue(), hfsVmId);
        Assert.assertEquals(detailedVm.dataCenter, credit.dataCenter);
    }

    @Test
    public void testShopperGetVmWithDetails() {
        testGetVmWithDetails();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetVmWithDetails() {
        user = GDUserMock.createShopper("shopperX");
        testGetVmWithDetails();
    }

    @Test
    public void testAdminGetVmWithDetails() {
        user = GDUserMock.createAdmin();
        testGetVmWithDetails();
    }

    // === getHfsDetails Tests ===
    @Test
    public void testShopperGetHfsDetails() {
        VirtualMachine vm = createTestVm();

        Vm hfsvm = getVmResource().getMoreDetails(vm.vmId);
        Assert.assertEquals(hfsvm.vmId, hfsVmId);
    }

}
