package com.godaddy.vps4.phase2;

import java.time.Instant;
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

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineType;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VirtualMachineDetails;
import com.godaddy.vps4.web.vm.VirtualMachineWithDetails;
import com.godaddy.vps4.web.vm.VmAction;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.vhfs.vm.Vm;

public class VmResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject VirtualMachineService virtualMachineService;

    private GDUser user;
    private long hfsVmId = 98765;
    private SchedulerWebService schedulerWebService = Mockito.mock(SchedulerWebService.class);

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(SchedulerWebService.class).toInstance(schedulerWebService);
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
        long ipAddressId = SqlTestData.getNextIpAddressId(dataSource);
        SqlTestData.insertTestIp(ipAddressId, vm.vmId, "127.0.0." + ipAddressId, IpAddressType.PRIMARY, dataSource);
        vm = virtualMachineService.getVirtualMachine(vm.vmId);
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
    public void testShopperGetRemovedVm() {
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
        Assert.assertNotNull(vmAction.commandId);
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
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null);
        Assert.assertEquals(1, vms.size());
        Assert.assertEquals(vms.get(0).orionGuid, vm.orionGuid);
    }
    
    @Test
    public void testShopperGetZombieVirtualMachines() {
        VirtualMachine vm = createTestVm();
        virtualMachineService.setVmZombie(vm.vmId);

        user = GDUserMock.createShopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ZOMBIE, user.getShopperId(), null, null, null);
        Assert.assertEquals(1, vms.size());
        Assert.assertEquals(vms.get(0).orionGuid, vm.orionGuid);
    }

    @Test
    public void testShopperGetVirtualMachinesEmpty() {
        user = GDUserMock.createShopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null);
        Assert.assertTrue(vms.isEmpty());
    }

    @Test
    public void testUnauthorizedShopperGetVirtualMachines() {
        createTestVm();

        user = GDUserMock.createShopper("shopperX");
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null);
        Assert.assertTrue(vms.isEmpty());
    }

    @Test
    public void testGetVmByIpAddress() {
        VirtualMachine vm = createTestVm();
        user = GDUserMock.createEmployee();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(null, null, vm.primaryIpAddress.ipAddress, null, null);

        Assert.assertEquals(vm.vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByOrionGuid() {
        VirtualMachine vm = createTestVm();
        user = GDUserMock.createEmployee();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(null, null, null, vm.orionGuid, null);

        Assert.assertEquals(vm.vmId, vms.get(0).vmId);
    }

    @Test
    public void testGetVmByHfsVmId() {
        VirtualMachine vm = createTestVm();
        user = GDUserMock.createEmployee();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(null, null, null, null, vm.hfsVmId);

        Assert.assertEquals(vm.vmId, vms.get(0).vmId);
    }

    @Test
    public void testE2SGetVirtualMachines() {
        VirtualMachine vm = createTestVm();

        user = GDUserMock.createEmployee2Shopper();
        List<VirtualMachine> vms = getVmResource().getVirtualMachines(VirtualMachineType.ACTIVE, user.getShopperId(), null, null, null);
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

        VirtualMachineWithDetails detailedVm = getVmResource().getVirtualMachineWithDetails(vm.vmId);
        Assert.assertEquals(detailedVm.orionGuid, vm.orionGuid);
        Assert.assertEquals(detailedVm.virtualMachineDetails.vmId.longValue(), hfsVmId);
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

    @Test
    public void testNoScheduledBackupId(){
        VirtualMachine vm = createTestVm();
        VirtualMachineWithDetails detailedVm = getVmResource().getVirtualMachineWithDetails(vm.vmId);
        Assert.assertEquals(0, detailedVm.autoSnapshots.copiesToRetain);
        Assert.assertEquals(0, detailedVm.autoSnapshots.repeatIntervalInDays);
        Assert.assertEquals(null, detailedVm.autoSnapshots.nextAt);
    }

    @Test
    public void testNoScheduledBackupJob(){
        VirtualMachine vm = createTestVm();
        UUID jobId = UUID.randomUUID();
        virtualMachineService.setBackupJobId(vm.vmId, jobId);
        Mockito.when(schedulerWebService.getJob("vps4", "backups", jobId)).thenReturn(null);
        VirtualMachineWithDetails detailedVm = getVmResource().getVirtualMachineWithDetails(vm.vmId);
        Assert.assertEquals(0, detailedVm.autoSnapshots.copiesToRetain);
        Assert.assertEquals(0, detailedVm.autoSnapshots.repeatIntervalInDays);
        Assert.assertEquals(null, detailedVm.autoSnapshots.nextAt);
    }

    @Test
    public void testAutoBackupScheduled(){
        Instant nextRun = Instant.now();
        JobRequest jobRequest = new JobRequest();
        jobRequest.repeatIntervalInDays = 7;
        SchedulerJobDetail jobDetail = new SchedulerJobDetail(UUID.randomUUID(), nextRun, jobRequest);
        VirtualMachine vm = createTestVm();
        virtualMachineService.setBackupJobId(vm.vmId, jobDetail.id);
        Mockito.when(schedulerWebService.getJob("vps4", "backups", jobDetail.id)).thenReturn(jobDetail);
        VirtualMachineWithDetails detailedVm = getVmResource().getVirtualMachineWithDetails(vm.vmId);
        Assert.assertEquals(1, detailedVm.autoSnapshots.copiesToRetain);
        Assert.assertEquals(7, detailedVm.autoSnapshots.repeatIntervalInDays);
        Assert.assertEquals(nextRun, detailedVm.autoSnapshots.nextAt);
    }



    // === getHfsDetails Tests ===
    @Test
    public void testShopperGetHfsDetails() {
        VirtualMachine vm = createTestVm();

        Vm hfsvm = getVmResource().getMoreDetails(vm.vmId);
        Assert.assertEquals(hfsvm.vmId, hfsVmId);
    }

}
