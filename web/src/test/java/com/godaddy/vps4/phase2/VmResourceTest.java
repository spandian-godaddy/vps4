package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject VirtualMachineService virtualMachineService;

    private GDUser user;
    private final SchedulerWebService schedulerWebService = Mockito.mock(SchedulerWebService.class);
    private final MailRelayService mailRelayService = mock(MailRelayService.class);

    private final Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(SchedulerWebService.class).toInstance(schedulerWebService);
                    bind(MailRelayService.class).toInstance(mailRelayService);
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
        when(mailRelayService.getMailRelay(anyString())).thenReturn(new MailRelay());
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmResource getVmResource() {
        return injector.getInstance(VmResource.class);
    }

    private VirtualMachine createTestVm() {
        // virtual servers are any tier < 60
        return createTestServer(10, 1);
    }

    private VirtualMachine createTestVmCustomDc(int dataCenterId) {
        return createTestServer(10, dataCenterId);
    }

    private VirtualMachine createTestDedicated() {
        // dedicated servers are any tier >= 60
        return createTestServer(60, 1);
    }

    private VirtualMachine createTestServer(int tier, int dataCenterId) {
        String imageName = tier < 60 ? "hfs-centos-7" : "centos7_64";
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1", UUID.randomUUID());
        VirtualMachine server = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource, imageName, tier, dataCenterId);
        long hfsAddressId = SqlTestData.getNextHfsAddressId(dataSource);
        SqlTestData.insertTestIp(hfsAddressId, server.vmId, createTestIP(), IpAddressType.PRIMARY, dataSource);
        server = virtualMachineService.getVirtualMachine(server.vmId);
        return server;
    }

    private String createTestIP() {
        int p1 = (int) (Math.random() * 255);
        int p2 = (int) (Math.random() * 255);
        return String.format("192.168.%d.%d", p1, p2);
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

    @Test
    public void testShopperGetSuspendedVm() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.SUSPENDED);
        VirtualMachine vm = createTestVm();

        user = GDUserMock.createShopper();
        try {
            getVmResource().getVm(vm.vmId);
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_SUSPENDED", e.getId());
        }
    }

    @Test
    public void testShopperGetRemovedAccountVm() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.REMOVED);
        VirtualMachine vm = createTestVm();

        user = GDUserMock.createShopper();
        try {
            getVmResource().getVm(vm.vmId);
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("ACCOUNT_REMOVED", e.getId());
        }
    }

    @Test
    public void testAdminGetRemovedAccountVm() {
        Phase2ExternalsModule.mockVmCredit(AccountStatus.REMOVED);
        VirtualMachine vm = createTestVm();
        UUID expectedGuid = vm.orionGuid;

        user = GDUserMock.createAdmin();
        vm = getVmResource().getVm(vm.vmId);
        Assert.assertEquals(expectedGuid, vm.orionGuid);
    }

    @Test(expected = NotFoundException.class)
    public void testNoVmGetVm() {
        user = GDUserMock.createShopper();
        UUID noSuchVmId = UUID.randomUUID();

        getVmResource().getVm(noSuchVmId);
    }

    @Test
    public void testShopperGetDeletedVm() {
        VirtualMachine vm = createTestVm();
        SqlTestData.invalidateTestVm(vm.vmId, dataSource);

        user = GDUserMock.createShopper();
        try {
            getVmResource().getVm(vm.vmId);
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertEquals("VM_DELETED", e.getId());
        }
    }

    @Test
    public void testAdminGetDeletedVm() {
        VirtualMachine vm = createTestVm();
        SqlTestData.invalidateTestVm(vm.vmId, dataSource);
        UUID expectedGuid = vm.orionGuid;

        user = GDUserMock.createAdmin();
        vm = getVmResource().getVm(vm.vmId);
        Assert.assertEquals(expectedGuid, vm.orionGuid);
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

        VmAction vmAction = getVmResource().startVm(vm.vmId);
        Assert.assertNotNull(vmAction.commandId);
    }

    @Test
    public void testShopperStartVm() {
        Phase2ExternalsModule.mockHfsVm("STOPPED");
        startVm();
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperStartVm() {
        user = GDUserMock.createShopper("shopperX");
        Phase2ExternalsModule.mockHfsVm("STOPPED");
        startVm();
    }

    @Test
    public void testAdminStartVm() {
        user = GDUserMock.createAdmin();
        Phase2ExternalsModule.mockHfsVm("STOPPED");
        startVm();
    }

    @Test
    public void testStartUnknownStatusVm() {
        Phase2ExternalsModule.mockHfsVm("UNKNOWN");
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
        Phase2ExternalsModule.mockHfsVm("STOPPED");
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

        VmAction vmAction = getVmResource().stopVm(vm.vmId);
        Assert.assertNotNull(vmAction.commandId);
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
    public void testStopUnknownStatusVm() {
        Phase2ExternalsModule.mockHfsVm("UNKNOWN");
        testStopVm();
    }

    @Test
    public void testStopInactiveVm() {
        Phase2ExternalsModule.mockHfsVm("STOPPED");
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

        VmAction vmAction = getVmResource().restartVm(vm.vmId);
        assertNotNull(vmAction.commandId);
        assertEquals(ActionType.RESTART_VM, vmAction.type);
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
    public void testRestartUnknownStatusVm() {
        Phase2ExternalsModule.mockHfsVm("UNKNOWN");
        testRestartVm();
    }

    @Test
    public void testRestartInactiveVm() {
        Phase2ExternalsModule.mockHfsVm("STOPPED");
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

        VmAction vmAction = getVmResource().restartVm(vm.vmId);
        Assert.assertNotNull(vmAction.commandId);
        try {
            getVmResource().stopVm(vm.vmId);
            Assert.fail("Exception not thrown");
        } catch (Vps4Exception e) {
            Assert.assertNotNull(vmAction.commandId);
        }
    }

    // === reboot Dedicated Vm Test ===
    public void testRebootDedicatedVm() {
        VirtualMachine vm = createTestDedicated();

        VmAction vmAction = getVmResource().restartVm(vm.vmId);
        assertNotNull(vmAction.commandId);
        assertEquals(ActionType.POWER_CYCLE, vmAction.type);
    }

    @Test
    public void testShopperRebootDedicatedVm() {
        testRebootDedicatedVm();
    }

    @Test
    public void testDoubleRebootDedicatedVm() {
        VirtualMachine vm = createTestDedicated();

        getVmResource().restartVm(vm.vmId);
        try {
            getVmResource().restartVm(vm.vmId);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    // === destroyVm Tests ===
    public void testDestroyVm() {
        VirtualMachine vm = createTestVm();

        VmAction vmAction = getVmResource().destroyVm(vm.vmId);
        Assert.assertNotNull(vmAction.commandId);
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
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null, null);
        Assert.assertEquals(1, vms.size());
        Assert.assertEquals(vms.get(0).orionGuid, vm.orionGuid);
    }

    @Test
    public void testShopperGetZombieVirtualMachines() {
        VirtualMachine vm = createTestVm();
        virtualMachineService.setVmZombie(vm.vmId);

        user = GDUserMock.createShopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ZOMBIE, user.getShopperId(), null, null, null, null);
        Assert.assertEquals(1, vms.size());
        Assert.assertEquals(vms.get(0).orionGuid, vm.orionGuid);
    }

    @Test
    public void testEmployeeShopperGetVirtualMachinesEmpty() {
        user = GDUserMock.createEmployee();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), "0.2.0.1", null, null, null);
        Assert.assertTrue(vms.isEmpty());
    }

    @Test
    public void testEmployeeFakeShopperGetVirtualMachinesEmpty() {
        user = GDUserMock.createEmployee();
        String shopperId = "FakeShopper";
        when(userService.getUser(user.getShopperId())).thenReturn(null, (Vps4User) null);
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, shopperId, null, null, null, null);
        Assert.assertTrue(vms.isEmpty());
    }

    @Test
    public void testShopperGetVirtualMachinesEmpty() {
        user = GDUserMock.createShopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null, null);
        Assert.assertTrue(vms.isEmpty());
    }

    @Test
    public void testUnauthorizedShopperGetVirtualMachines() {
        createTestVm();

        user = GDUserMock.createShopper("shopperX");
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null, null);
        Assert.assertTrue(vms.isEmpty());
    }

    @Test
    public void testGetVmByIpAddress() {
        VirtualMachine vm = createTestVm();
        user = GDUserMock.createEmployee();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(null, null, vm.primaryIpAddress.ipAddress, null, null, null);

        Assert.assertEquals(vm.vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByOrionGuid() {
        VirtualMachine vm = createTestVm();
        user = GDUserMock.createEmployee();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(null, null, null, vm.orionGuid, null, null);

        Assert.assertEquals(vm.vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByHfsVmId() {
        VirtualMachine vm = createTestVm();
        user = GDUserMock.createEmployee();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(null, null, null, null, vm.hfsVmId, null);

        Assert.assertEquals(vm.vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByDcIdAndShopper() {
        VirtualMachine vm = createTestVm();
        createTestVmCustomDc(3);
        user = GDUserMock.createShopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(null, user.getShopperId(), null, null, null, 1);

        Assert.assertEquals(1, vms.size());
        Assert.assertEquals(vm.vmId, vms.get(0).vmId);
    }

    @Test
    public void testE2SGetVirtualMachines() {
        VirtualMachine vm = createTestVm();
        VirtualMachine vm2 = createTestVm();
        SqlTestData.insertTestVm(UUID.randomUUID(), dataSource);
        Set<UUID> expectedOrionGuids = new HashSet<>();
        expectedOrionGuids.add(vm.orionGuid);
        expectedOrionGuids.add(vm2.orionGuid);

        user = GDUserMock.createEmployee2Shopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, null, null, null, null, null);
        Assert.assertEquals(2, vms.size());
        Set<UUID> actualOrionGuids = new HashSet<>();
        actualOrionGuids.add(vms.get(0).orionGuid);
        actualOrionGuids.add(vms.get(1).orionGuid);

        Assert.assertEquals(expectedOrionGuids, actualOrionGuids);
    }

}
