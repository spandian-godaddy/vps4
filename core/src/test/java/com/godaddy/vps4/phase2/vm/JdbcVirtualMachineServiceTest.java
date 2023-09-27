package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
import com.godaddy.vps4.vm.InsertVirtualMachineParameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditHistory;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VirtualMachineType;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class JdbcVirtualMachineServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    NetworkService networkService = new JdbcNetworkService(dataSource);
    VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
    ProjectService projectService = new JdbcProjectService(dataSource);
    Vps4UserService vps4UserService = new JdbcVps4UserService(dataSource);

    private UUID orionGuid = UUID.randomUUID();
    private final String INFINITY = "+292278994-08-16T23:00:00Z";
    List<VirtualMachine> virtualMachines;
    List<UUID> vmCredits;
    Vps4User vps4User = vps4UserService.getOrCreateUserForShopper("TestUser", "1", UUID.randomUUID());
    Vps4User vps4User2 = vps4UserService.getOrCreateUserForShopper("TestUser2", "1", UUID.randomUUID());

    @Before
    public void setup() {
        List<VirtualMachine> oldVms = virtualMachineService.getVirtualMachines(VirtualMachineType.ACTIVE, vps4User.getId(), null, null, null, null, null);
        for (VirtualMachine oldVm : oldVms) {
            SqlTestData.cleanupTestVmAndRelatedData(oldVm.vmId, dataSource);
        }

        virtualMachines = new ArrayList<>();
        vmCredits = new ArrayList<>();
    }

    @After
    public void cleanup() {
        for (VirtualMachine vm : virtualMachines) {
            SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        }
        SqlTestData.deleteVps4User(vps4User.getId(), dataSource);
        SqlTestData.deleteVps4User(vps4User2.getId(), dataSource);
    }

    @Test
    public void testHasCPanel() {
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, "testServer", 10, 1, "centos-7-cpanel-11");

        virtualMachineService.provisionVirtualMachine(params);

        VirtualMachine vm = virtualMachineService.getVirtualMachines(VirtualMachineType.ACTIVE, vps4User.getId(), null, null, null, null, null).get(0);
        virtualMachines.add(vm);
        Assert.assertTrue(virtualMachineService.virtualMachineHasCpanel(vm.vmId));
    }

    @Test
    public void testHasPleskPanel() {
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, "testServer", 10, 1, "windows-2012r2-plesk-12.5");

        virtualMachineService.provisionVirtualMachine(params);
        VirtualMachine vm = virtualMachineService.getVirtualMachines(VirtualMachineType.ACTIVE, vps4User.getId(), null, null, null, null, null).get(0);
        virtualMachines.add(vm);
        Assert.assertTrue(virtualMachineService.virtualMachineHasPlesk(vm.vmId));
    }

    @Test
    public void testService() {
        String name = "testServer";
        int tier = 10;
        int specId = virtualMachineService.getSpec(tier, ServerType.Platform.OPENSTACK.getplatformId()).specId;

        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, name, tier, 1, "centos-7");

        virtualMachineService.provisionVirtualMachine(params);
        List<VirtualMachine> vms = virtualMachineService.getVirtualMachines(VirtualMachineType.ACTIVE, vps4User.getId(), null, null, null, null, null);
        assertEquals(1, vms.size());

        VirtualMachine vm = vms.get(0);

        virtualMachines.add(vm);
        long hfsVmId = SqlTestData.getNextHfsVmId(dataSource);
        virtualMachineService.addHfsVmIdToVirtualMachine(vm.vmId, hfsVmId);

        vm = virtualMachineService.getVirtualMachine(vm.vmId);
        verifyVm(name, specId, hfsVmId, vm);

        vm = virtualMachineService.getVirtualMachine(hfsVmId);
        verifyVm(name, specId, hfsVmId, vm);
    }

    private void verifyVm(String name, int specId, long hfsVmId, VirtualMachine vm) {
        assertNotNull(vm);
        assertEquals(hfsVmId, vm.hfsVmId);
        assertEquals(name, vm.name);
        assertEquals(specId, vm.spec.specId);
        assertEquals("centos-7", vm.image.hfsName);
        assertEquals("CentOS 7", vm.image.imageName);
    }

    @Test
    public void testGetVirtualMachineByOrionGuid() {
        UUID orionGuid = UUID.randomUUID();
        VirtualMachine testVm = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        virtualMachines.add(testVm);
        VirtualMachine actualVm = virtualMachineService.getVirtualMachines(null, null, null, orionGuid, null, null, null).get(0);
        assertEquals(testVm.vmId, actualVm.vmId);
    }

    @Test
    public void testGetVirtualMachineByHfsVmId() {
        VirtualMachine testVm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User.getId());
        virtualMachines.add(testVm);
        VirtualMachine actualVm = virtualMachineService.getVirtualMachine(testVm.hfsVmId);
        assertEquals(testVm.hfsVmId, actualVm.hfsVmId);
    }

    @Test
    public void testGetVirtualMachineByPrimaryIpAddress() {
        UUID orionGuid = UUID.randomUUID();
        VirtualMachine testVm = SqlTestData.insertTestVmWithIp(orionGuid, dataSource, vps4User.getId());
        virtualMachines.add(testVm);
        VirtualMachine actualVm = virtualMachineService.getVirtualMachines(null, null, testVm.primaryIpAddress.ipAddress, null, null, null, null).get(0);
        assertEquals(testVm.hfsVmId, actualVm.hfsVmId);
    }

    @Test
    public void testGetVirtualMachineBySecondaryIpAddress() {
        UUID orionGuid = UUID.randomUUID();
        VirtualMachine testVm = SqlTestData.insertTestVmWithIp(orionGuid, dataSource, vps4User.getId());
        SqlTestData.insertSecondaryIpToVm(testVm.vmId, dataSource);
        virtualMachines.add(testVm);
        IpAddress additionalIp = networkService.getVmActiveSecondaryAddresses(testVm.hfsVmId).get(0);
        VirtualMachine actualVm = virtualMachineService.getVirtualMachines(null, null, additionalIp.ipAddress , null, null, null, null).get(0);
        assertEquals(testVm.hfsVmId, actualVm.hfsVmId);
    }

    @Test
    public void testGetVirtualMachinesByShopperId() {
        for (int i = 0; i < 3; i++) {
            VirtualMachine testVm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User.getId());
            virtualMachines.add(testVm);
        }
        List<VirtualMachine> actualVms = virtualMachineService.getVirtualMachines(null, vps4User.getId(), null, null, null, null, null);
        assertEquals(virtualMachines.size(), actualVms.size());
    }

    @Test
    public void testGetVirtualMachineByDcIdAndShopperId() {
        UUID orionGuid = UUID.randomUUID();
        VirtualMachine testVm = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        VirtualMachine testVmOtherDc = SqlTestData.insertTestVmCustomDc(orionGuid, dataSource, 3, vps4User.getId());
        virtualMachines.add(testVm);
        virtualMachines.add(testVmOtherDc);

        List<VirtualMachine> vms = virtualMachineService.getVirtualMachines(null, vps4User.getId(), null, null, 1, null, null);
        VirtualMachine actualVm = vms.get(0);
        assertEquals(1, vms.size());
        assertEquals(testVm.vmId, actualVm.vmId);
    }

    @Test
    public void testGetVirtualMachineByIpAndHfsVmId() {
        VirtualMachine testVm = null;
        for (int i = 0; i < 3; i++) {
            testVm = SqlTestData.insertTestVmWithIp(UUID.randomUUID(), dataSource, vps4User.getId());
            virtualMachines.add(testVm);
        }
        List<VirtualMachine> actualVms = virtualMachineService.getVirtualMachines(null, null, testVm.primaryIpAddress.ipAddress, null, null, null, Collections.singletonList(testVm.hfsVmId+1));
        assertEquals(0, actualVms.size());
    }

    @Test
    public void testGetVirtualMachinesByHfsVmIdAndUserId() {
        VirtualMachine testVm = null;
        for (int i = 0; i < 3; i++) {
            testVm = SqlTestData.insertTestVmWithIp(UUID.randomUUID(), dataSource, vps4User.getId());
            virtualMachines.add(testVm);
        }
        virtualMachines.add(SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User2.getId()));


        List<VirtualMachine> actualVms = virtualMachineService.getVirtualMachines(null, vps4User2.getId(),
                null, null, null, null, Collections.singletonList(testVm.hfsVmId));
        assertEquals(0, actualVms.size());

        actualVms = virtualMachineService.getVirtualMachines(null, vps4User.getId(), null, null,
                null, null, Collections.singletonList(testVm.hfsVmId));
        assertEquals(1, actualVms.size());
        assertEquals(testVm.vmId, actualVms.get(0).vmId);
    }

    @Test
    public void testGetVirtualMachinesByListOfHfsVmIds() {
        for (int i = 0; i < 3; i++) {
            VirtualMachine testVm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User.getId());
            virtualMachines.add(testVm);
        }
        List<Long> firstTwoHfsVms = Arrays.asList(virtualMachines.get(0).hfsVmId, virtualMachines.get(1).hfsVmId);
        List<VirtualMachine> actualVms = virtualMachineService.getVirtualMachines(null, null, null, null, null, null, firstTwoHfsVms);
        assertEquals(2, actualVms.size());
        assertEquals(virtualMachines.get(0).hfsVmId, actualVms.get(1).hfsVmId);
        assertEquals(virtualMachines.get(1).hfsVmId, actualVms.get(0
        ).hfsVmId);
    }

    @Test
    public void testGetCreditHistory() {
        UUID orionGuid = UUID.randomUUID();
        virtualMachines.add(SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId()));
        virtualMachines.add(SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId()));
        virtualMachines.add(SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User.getId()));
        virtualMachineService.setVmRemoved(virtualMachines.get(1).vmId);

        List<CreditHistory> creditHistories = virtualMachineService.getCreditHistory(orionGuid);
        assertEquals(virtualMachines.size()-1, creditHistories.size());
        for(int i = 0; i < creditHistories.size(); i++) {
            assertEquals(virtualMachines.get(i).vmId, creditHistories.get(i).vmId);
            assertEquals(virtualMachines.get(i).validOn, creditHistories.get(i).validOn);
            assertTrue(virtualMachines.get(i).validUntil.equals(creditHistories.get(i).validUntil) ||
                    creditHistories.get(i).validUntil.isBefore(virtualMachines.get(i).validUntil));
        }
    }

    @Test
    public void testProvisionVmCreatesId() {
        VirtualMachine virtualMachine = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        virtualMachines.add(virtualMachine);
        assertNotNull(virtualMachine);
        assertEquals(UUID.class, virtualMachine.vmId.getClass());
    }

    @Test
    public void testProvisionVmUsesValidSpec() {
        VirtualMachine virtualMachine = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        virtualMachines.add(virtualMachine);
        assertTrue(virtualMachine.spec.validUntil.isAfter(Instant.now()));
    }

    @Test
    public void testGetVirtualMachines() {
        List<UUID> createdVms = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            createdVms.add(UUID.randomUUID());
            virtualMachines.add(SqlTestData.insertTestVm(createdVms.get(i), dataSource, vps4User.getId()));
            vmCredits.add(UUID.randomUUID());
        }
        virtualMachineService.setVmRemoved(virtualMachines.get(1).vmId);
        createdVms.remove(virtualMachines.get(1).orionGuid);

        List<VirtualMachine> vms = virtualMachineService.getVirtualMachines(VirtualMachineType.ACTIVE, vps4User.getId(), null, null, null, null, null);
        List<UUID> vmGuids = vms.stream().map(vm -> vm.orionGuid).collect(Collectors.toList());
        for (UUID vm : createdVms)
            assertTrue(vmGuids.contains(vm));
        assertEquals(virtualMachines.size()-1, vms.size());
    }

    @Test
    public void testSetVmActive() {
        List<UUID> createdVms = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            createdVms.add(UUID.randomUUID());
            virtualMachines.add(SqlTestData.insertTestVm(createdVms.get(i), dataSource, vps4User.getId()));
            vmCredits.add(UUID.randomUUID());
        }

        virtualMachineService.setVmRemoved(virtualMachines.get(0).vmId);
        virtualMachineService.setVmRemoved(virtualMachines.get(1).vmId);
        virtualMachineService.setVmActive(virtualMachines.get(0).vmId);
        List<VirtualMachine> vms = virtualMachineService.getVirtualMachines(VirtualMachineType.ACTIVE, vps4User.getId(), null, null, null, null, null);
        assertEquals(2, vms.size());
    }

    @Test
    public void testGetZombieVirtualMachines() {
        List<UUID> createdVms = new ArrayList<>();
        for(int i = 0; i < 4; i++) {
            createdVms.add(UUID.randomUUID());
            virtualMachines.add(SqlTestData.insertTestVm(createdVms.get(i), dataSource, vps4User.getId()));
            vmCredits.add(UUID.randomUUID());
        }
        virtualMachineService.setVmRemoved(virtualMachines.get(1).vmId);
        virtualMachineService.setVmCanceled(virtualMachines.get(0).vmId);
        virtualMachineService.setVmCanceled(virtualMachines.get(2).vmId);


        List<VirtualMachine> vms = virtualMachineService.getVirtualMachines(VirtualMachineType.ZOMBIE, vps4User.getId(), null, null, null, null, null);
        List<UUID> vmGuids = vms.stream().map(vm -> vm.vmId).collect(Collectors.toList());
        assertTrue(vmGuids.contains(virtualMachines.get(0).vmId));
        assertTrue(vmGuids.contains(virtualMachines.get(2).vmId));
    }

    @Test
    public void testGetPendingSnapshotActionIdByVmId() {
        virtualMachines.add(SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId()));
        VirtualMachine testVm = virtualMachines.get(0);

        // No snapshots
        Assert.assertNull(virtualMachineService.getPendingSnapshotActionIdByVmId(testVm.vmId));

        Snapshot testSnapshot = new Snapshot(
                UUID.randomUUID(),
                testVm.projectId,
                testVm.vmId,
                "test-snapshot",
                SnapshotStatus.LIVE,
                Instant.now(),
                null,
                "test-imageid",
                (long) (Math.random() * 100000),
                SnapshotType.ON_DEMAND
        );
        SqlTestData.insertTestSnapshot(testSnapshot, dataSource);

        // Snapshot with completed create action - not pending
        SqlTestData.insertTestSnapshotAction(testSnapshot.id, ActionType.CREATE_SNAPSHOT,
                ActionStatus.COMPLETE, dataSource);
        Assert.assertNull(virtualMachineService.getPendingSnapshotActionIdByVmId(testVm.vmId));

        // Snapshot with pending create action
        SqlTestData.insertTestSnapshotAction(testSnapshot.id, ActionType.CREATE_SNAPSHOT,
                ActionStatus.IN_PROGRESS, dataSource);
        Assert.assertNotNull(virtualMachineService.getPendingSnapshotActionIdByVmId(testVm.vmId));
    }

    @Test
    public void testVmOsDistro() {
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, "testServer", 10, 1, "hfs-centos-7-cpanel-11");
        VirtualMachine vm = virtualMachineService.provisionVirtualMachine(params);
        virtualMachines.add(vm);

        Assert.assertEquals( "centos-7", virtualMachineService.getOSDistro(vm.vmId));
    }

    @Test
    public void testVmOsDistroIspConfig() {
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, "testServer", 10, 1, "vps4-ubuntu-1604-ispconfig-3");
        VirtualMachine vm = virtualMachineService.provisionVirtualMachine(params);
        virtualMachines.add(vm);

        System.out.println(virtualMachineService.getOSDistro(vm.vmId));
        Assert.assertEquals( "ubuntu-1604", virtualMachineService.getOSDistro(vm.vmId));
    }

    @Test
    public void testUpdateManagedLevel() {
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        virtualMachines.add(vm);

        Assert.assertEquals(0,  vm.managedLevel);

        Map<String, Object> paramsToUpdate = new HashMap<>();
        paramsToUpdate.put("managed_level", 2);
        virtualMachineService.updateVirtualMachine(vm.vmId, paramsToUpdate);
        vm = virtualMachineService.getVirtualMachine(vm.vmId);

        Assert.assertEquals(2,  vm.managedLevel);
    }

    @Test
    public void testVmNotExistsReturnsNull() {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(UUID.randomUUID());
        Assert.assertNull(vm);
    }

    @Test
    public void testSetVmCanceled() {
        VirtualMachine expectedVm = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        virtualMachines.add(expectedVm);

        Instant maxPsqlTimestamp = expectedVm.canceled;
        virtualMachineService.setVmCanceled(expectedVm.vmId);
        VirtualMachine actualVm = virtualMachineService.getVirtualMachine(expectedVm.vmId);

        Assert.assertTrue("VM Canceled not set properly", actualVm.canceled.isBefore(maxPsqlTimestamp));
        Assert.assertTrue("VM Canceled must be after Valid On", actualVm.validOn.isBefore(actualVm.canceled));
        Assert.assertTrue("VM Canceled is after Valid Until", actualVm.canceled.isBefore(actualVm.validUntil));
    }

    @Test
    public void testReviveZombie() {
        VirtualMachine expectedVm = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        virtualMachineService.setVmCanceled(expectedVm.vmId);
        virtualMachines.add(expectedVm);

        UUID newOrionGuid = UUID.randomUUID();
        virtualMachineService.reviveZombieVm(expectedVm.vmId, newOrionGuid);
        VirtualMachine actualVm = virtualMachineService.getVirtualMachine(expectedVm.vmId);

        Assert.assertEquals(INFINITY, actualVm.canceled.toString());
        Assert.assertEquals(newOrionGuid, actualVm.orionGuid);
    }

    @Test
    public void testReviveZombieVmWithoutNewCredit() {
        VirtualMachine expectedVm = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        virtualMachineService.setVmCanceled(expectedVm.vmId);
        virtualMachines.add(expectedVm);

        virtualMachineService.clearVmCanceled(expectedVm.vmId);
        VirtualMachine actualVm = virtualMachineService.getVirtualMachine(expectedVm.vmId);

        Assert.assertEquals(INFINITY, actualVm.canceled.toString());
    }

    @Test
    public void testGetActiveServerCountByTiers() {
        Map<Integer, Integer> currentCount = virtualMachineService.getActiveServerCountByTiers();

        for (int i = 0; i < 3; i++) {
            VirtualMachine testVm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User.getId());
            virtualMachines.add(testVm);
        }

        Map<Integer, Integer> activeServerCount = virtualMachineService.getActiveServerCountByTiers();
        assertEquals(currentCount.getOrDefault(10, 0).intValue() + 3, activeServerCount.get(10).intValue());
    }

    @Test
    public void testGetZombieServerCountByTiers() {
        for (int i = 0; i < 3; i++) {
            VirtualMachine testVm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User.getId());
            virtualMachines.add(testVm);
        }
        virtualMachineService.setVmCanceled(virtualMachines.get(0).vmId);
        virtualMachineService.setVmCanceled(virtualMachines.get(1).vmId);

        Map<Integer, Integer> zombieServerCount = virtualMachineService.getZombieServerCountByTiers();
        assertEquals(2, zombieServerCount.get(10).intValue());
    }

    @Test
    public void testSetMonitoringPlanFeature(){
        VirtualMachine testVm = SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User.getId());
        virtualMachines.add(testVm);
        virtualMachineService.setMonitoringPlanFeature(testVm.vmId, true);
        assertTrue(virtualMachineService.getMonitoringPlanFeature(testVm.vmId));
    }

    @Test
    public void testImportVirtualMachine() {
        Project project = projectService.createProjectAndPrivilegeWithSgid("testProjectForImportVm", vps4User.getId(), "testProjectForImportVm");
        InsertVirtualMachineParameters parameters =
                new InsertVirtualMachineParameters(-1,
                                                   UUID.randomUUID(),
                                                   "testImportServer",
                                                   project.getProjectId(),
                                                   10,
                                                   1,
                                                   1,
                                                   null);
        VirtualMachine testVm2 = virtualMachineService.importVirtualMachine(parameters);
        UUID importedVm = virtualMachineService.getImportedVm(testVm2.vmId);
        virtualMachines.add(testVm2);
        assertNotNull(testVm2);
        assertNotNull(importedVm);
    }

    @Test
    public void testGetImportedVirtualMachineReturnsNull() {
        UUID importedVm = virtualMachineService.getImportedVm(UUID.randomUUID());
        assertNull(importedVm);
    }

    @Test
    public void testGetVirtualMachineReturnsIpLimit() {
        UUID orionGuid = UUID.randomUUID();
        VirtualMachine testVm = SqlTestData.insertTestVmWithIp(orionGuid, dataSource, vps4User.getId());
        virtualMachines.add(testVm);
        VirtualMachine actualVm = virtualMachineService.getVirtualMachines(null, null, null, orionGuid, null, null, null).get(0);
        assertNotNull(actualVm.spec.ipAddressLimit);
    }

    @Test
    public void testGetVirtualMachineByPlatform() {
        String platform = "OPTIMIZED_HOSTING";
        virtualMachines.add(SqlTestData.insertTestVmCustomSpec(UUID.randomUUID(), dataSource, vps4User.getId(), 10, "hfs-centos7"));
        virtualMachines.add(SqlTestData.insertTestVm(UUID.randomUUID(), dataSource, vps4User.getId()));

        List<VirtualMachine> vms = virtualMachineService.getVirtualMachines(null, vps4User.getId(), null, null, null, platform, null);
        VirtualMachine vm = vms.get(0);

        assertEquals(1, vms.size());
        assertEquals(platform, vm.spec.serverType.platform.name());
    }
}
