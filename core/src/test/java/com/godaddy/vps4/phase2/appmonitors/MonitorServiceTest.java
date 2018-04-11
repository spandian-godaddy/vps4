package com.godaddy.vps4.phase2.appmonitors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.appmonitors.jdbc.JdbcMonitorService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Vps4ReportsDataSource;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MonitorServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource reportsDataSource = injector.getInstance(Key.get(DataSource.class, Vps4ReportsDataSource.class));
    MonitorService provisioningMonitorService = new JdbcMonitorService(reportsDataSource);

    private UUID orionGuid = UUID.randomUUID();
    private VirtualMachine vm1, vm2, vm3, vm4, vm5, vm6, vm7, vm8;
    private Vps4User vps4User;
    private Vps4UserService vps4UserService;
    private Snapshot testSnapshotVm6, testSnapshotVm7;

    @Before
    public void setupService() {
        vps4UserService = new JdbcVps4UserService(reportsDataSource);
        vps4User = vps4UserService.getOrCreateUserForShopper("FakeShopper", "1");
        vm1 = SqlTestData.insertTestVm(orionGuid, reportsDataSource);
        createActionWithDate(vm1.vmId, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, Instant.now().minus(Duration.ofMinutes(61)), vps4User.getId(), reportsDataSource);
        vm2 = SqlTestData.insertTestVm(orionGuid, reportsDataSource);
        createActionWithDate(vm2.vmId, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, Instant.now().minus(Duration.ofMinutes(10)), vps4User.getId(), reportsDataSource);
        vm3 = SqlTestData.insertTestVm(orionGuid, reportsDataSource);
        createActionWithDate(vm3.vmId, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, Instant.now().minus(Duration.ofMinutes(90)), vps4User.getId(), reportsDataSource);
        vm4 = SqlTestData.insertTestVm(orionGuid, reportsDataSource);
        createActionWithDate(vm4.vmId, ActionType.ENABLE_ADMIN_ACCESS, ActionStatus.IN_PROGRESS, Instant.now().minus(Duration.ofMinutes(90)), vps4User.getId(), reportsDataSource);
        vm5 = SqlTestData.insertTestVm(orionGuid, reportsDataSource);
        createActionWithDate(vm5.vmId, ActionType.CREATE_VM, ActionStatus.ERROR, Instant.now().minus(Duration.ofMinutes(90)), vps4User.getId(), reportsDataSource);
        vm6 = SqlTestData.insertTestVm(orionGuid, reportsDataSource);
        testSnapshotVm6 = new Snapshot(
                UUID.randomUUID(),
                vm6.projectId,
                vm6.vmId,
                "fake-snapshot-1",
                SnapshotStatus.LIVE,
                Instant.now(),
                null,
                "fake-imageid",
                (int) (Math.random() * 100000),
                SnapshotType.AUTOMATIC
        );
        SqlTestData.insertTestSnapshot(testSnapshotVm6, reportsDataSource);
        createSnapshotActionWithDate(testSnapshotVm6.id, ActionType.CREATE_SNAPSHOT, ActionStatus.IN_PROGRESS, Instant.now().minus(Duration.ofMinutes(125)), vps4User.getId(), reportsDataSource);
        vm7 = SqlTestData.insertTestVm(orionGuid, reportsDataSource);
        testSnapshotVm7 = new Snapshot(
                UUID.randomUUID(),
                vm7.projectId,
                vm7.vmId,
                "fake-snapshot-1",
                SnapshotStatus.LIVE,
                Instant.now(),
                null,
                "fake-imageid",
                (int) (Math.random() * 100000),
                SnapshotType.AUTOMATIC
        );
        SqlTestData.insertTestSnapshot(testSnapshotVm7, reportsDataSource);
        createSnapshotActionWithDate(testSnapshotVm7.id, ActionType.CREATE_SNAPSHOT, ActionStatus.IN_PROGRESS, Instant.now().minus(Duration.ofMinutes(60)), vps4User.getId(), reportsDataSource);
        vm8 = SqlTestData.insertTestVm(orionGuid, reportsDataSource);
        createActionWithDate(vm8.vmId, ActionType.CREATE_VM, ActionStatus.NEW, Instant.now().minus(Duration.ofMinutes(125)), vps4User.getId(), reportsDataSource);
    }

    @After
    public void cleanupService() {
        SqlTestData.cleanupTestVmAndRelatedData(vm1.vmId, reportsDataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm2.vmId, reportsDataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm3.vmId, reportsDataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm4.vmId, reportsDataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm5.vmId, reportsDataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm6.vmId, reportsDataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm7.vmId, reportsDataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm8.vmId, reportsDataSource);
        SqlTestData.deleteVps4User(vps4User.getId(), reportsDataSource);
    }

    private static void createActionWithDate(UUID vmId, ActionType actionType, ActionStatus status, Instant created, long userId, @Vps4ReportsDataSource DataSource dataSource) {
        //TODO: when we convert to using the latest jdbc driver, the sql should allow taking java instant.
        Sql.with(dataSource).exec("INSERT INTO vm_action (vm_id, action_type_id, status_id, created, vps4_user_id, command_id) VALUES (?, ?, ?, ?, ?, ?)",
                null, vmId, actionType.getActionTypeId(), status.getStatusId(), Timestamp.from(created), userId, UUID.randomUUID());
    }

    private static void createSnapshotActionWithDate(UUID snapshotId, ActionType actionType, ActionStatus status, Instant created, long userId, @Vps4ReportsDataSource DataSource dataSource) {
        //TODO: when we convert to using the latest jdbc driver, the sql should allow taking java instant.
        Sql.with(dataSource).exec("INSERT INTO snapshot_action (snapshot_id, action_type_id, status_id, created, vps4_user_id, command_id) VALUES (?, ?, ?, ?, ?, ?)",
                null, snapshotId, actionType.getActionTypeId(), status.getStatusId(), Timestamp.from(created), userId, UUID.randomUUID());
    }

    @Test
    public void testGetVmsByActions() {
        List<VmActionData> problemVms = provisioningMonitorService.getVmsByActions(60, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS);
        assertNotNull(problemVms);
        // TODO: change the size to equal 2 when we move to newer version of jdbc driver, utc does not work as timestamp is not set with timezone
        assertTrue(String.format("Expected count of problem VM's does not match actual count of {%s} VM's.", problemVms.size()), problemVms.size() == 3);
        assertTrue("Expected vm id not present in list of problem VM's.", problemVms.stream().anyMatch(vm -> (vm.vmId.compareTo(vm1.vmId) == 0)));
        assertTrue("Expected vm id not present in list of problem VM's.", problemVms.stream().anyMatch(vm -> (vm.vmId.compareTo(vm3.vmId) == 0)));
    }

    @Test
    public void testGetVmsBySnapshotActions() {
        List<SnapshotActionData> problemVms = provisioningMonitorService.getVmsBySnapshotActions(120, ActionStatus.IN_PROGRESS, ActionStatus.ERROR);
        assertNotNull(problemVms);
        // TODO: change the size to equal 1 when we move to newer version of jdbc driver, utc does not work as timestamp is not set with timezone
        assertTrue(String.format("Expected count of problem VM's does not match actual count of {%s} VM's.", problemVms.size()), problemVms.size() == 2);
        assertTrue("Expected vm id not present in list of problem VM's.", problemVms.stream().anyMatch(vm -> (vm.snapshotId.compareTo(testSnapshotVm6.id) == 0)));
    }

    @Test
    public void testGetVmsPendingNewActions() {
        List<VmActionData> problemVms = provisioningMonitorService.getVmsByActionStatus(120, ActionStatus.NEW);
        assertNotNull(problemVms);
        // TODO: change the size to equal 1 when we move to newer version of jdbc driver, utc does not work as timestamp is not set with timezone
        assertTrue(String.format("Expected count of problem VM's does not match actual count of {%s} VM's.", problemVms.size()), problemVms.size() == 1);
        assertTrue("Expected vm id not present in list of problem VM's.", problemVms.stream().anyMatch(vm -> (vm.vmId.compareTo(vm8.vmId) == 0)));
    }

}
