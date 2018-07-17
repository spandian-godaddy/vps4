package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.json.simple.JSONObject;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVmActionService;


public class SqlTestData {
    public final static String TEST_VM_NAME = "testVirtualMachine";
    public final static String TEST_SNAPSHOT_NAME = "test-snapshot";
    public final static String TEST_VM_SGID = "vps4-testing-";
    public final static long hfsSnapshotId = 123;
    public final static String nfImageId = "test-imageid";
    public final static String INITIATED_BY = "tester";

    public static long getNextHfsVmId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(hfs_vm_id) as hfs_vm_id FROM virtual_machine",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("hfs_vm_id"))) + 1;
    }

    public static long getNextIpAddressId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(ip_address_id) as ip_address_id FROM ip_address",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("ip_address_id"))) + 1;
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, DataSource dataSource) {
        return insertTestVm(orionGuid, 1, dataSource);
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, long vps4UserId, DataSource dataSource){
        return insertTestVm(orionGuid, vps4UserId, dataSource, "centos-7");
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, long vps4UserId, DataSource dataSource, String imageName) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        long hfsVmId = getNextHfsVmId(dataSource);
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4UserId, 1, TEST_VM_SGID, orionGuid,
                TEST_VM_NAME, 10, 1, imageName);
        VirtualMachine virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        return virtualMachineService.getVirtualMachine(virtualMachine.vmId);
    }

    public static IpAddress insertTestIp(long ipAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType, DataSource dataSource){
        NetworkService networkService = new JdbcNetworkService(dataSource);
        networkService.createIpAddress(ipAddressId, vmId, ipAddress, ipAddressType);
        return networkService.getIpAddress(ipAddressId);
    }

    public static Action insertTestVmAction(UUID commandId, UUID vmId, ActionType actionType, DataSource dataSource) {
        ActionService actionService = new JdbcVmActionService(dataSource);
        long actionId = actionService.createAction(vmId, actionType, new JSONObject().toJSONString(), INITIATED_BY);
        actionService.tagWithCommand(actionId, commandId);
        return actionService.getAction(actionId);
    }

    public static void invalidateTestVm(UUID vmId, DataSource dataSource) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        virtualMachineService.setVmRemoved(vm.vmId);
    }

    public static Snapshot insertSnapshot(SnapshotService snapshotService, UUID vmId, long projectId, SnapshotType snapshotType) {
        UUID snapshotId = snapshotService.createSnapshot(projectId, vmId, TEST_SNAPSHOT_NAME, snapshotType);
        return snapshotService.getSnapshot(snapshotId);
    }

    public static Action insertTestSnapshotAction(ActionService snapshotActionService, UUID commandId,
                                                  UUID snapshotId, ActionType actionType, DataSource dataSource) {
        long actionId = snapshotActionService.createAction(
            snapshotId, actionType, new JSONObject().toJSONString(), INITIATED_BY);
        snapshotActionService.tagWithCommand(actionId, commandId);
        return snapshotActionService.getAction(actionId);
    }

    public static Snapshot insertSnapshotWithStatus(SnapshotService snapshotService, UUID vmId,
                                                long projectId, SnapshotStatus status, SnapshotType snapshotType) {
        Snapshot snapshot = insertSnapshot(snapshotService, vmId, projectId, snapshotType);
        snapshotService.updateSnapshotStatus(snapshot.id, status);
        snapshotService.updateHfsSnapshotId(snapshot.id, hfsSnapshotId);
        snapshotService.updateHfsImageId(snapshot.id, nfImageId);
        return snapshot;
    }

    public static void invalidateSnapshot(SnapshotService snapshotService, UUID snapshotId) {
        snapshotService.markSnapshotDestroyed(snapshotId);
    }

    public static void insertTestUser(VmUser user, DataSource dataSource) {
        Sql.with(dataSource).exec("INSERT INTO vm_user (name, admin_enabled, vm_id, vm_user_type_id) VALUES (?, ?, ?, ?)", null, user.username, user.adminEnabled, user.vmId, user.vmUserType.getVmUserTypeId());
    }

    public static void cleanupSqlTestData(DataSource dataSource) {
        String test_vm_condition = "v.name='" + TEST_VM_NAME + "'";
        String test_snapshot_condition = "s.name='" + TEST_SNAPSHOT_NAME + "'";
        String test_sgid_condition = "p.vhfs_sgid like '" + TEST_VM_SGID + "%'";

        Sql.with(dataSource).exec("DELETE FROM ip_address i USING virtual_machine v WHERE i.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM vm_user u USING virtual_machine v WHERE u.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM vm_action a USING virtual_machine v WHERE a.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM snapshot_action sa USING snapshot s WHERE sa.snapshot_id = s.id AND " + test_snapshot_condition, null);
        Sql.with(dataSource).exec("DELETE FROM snapshot_action sa USING virtual_machine v, snapshot s "
                + "WHERE sa.snapshot_id = s.id AND s.vm_id=v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM snapshot s USING virtual_machine v WHERE s.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine v USING project p WHERE v.project_id = p.project_id AND " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM user_project_privilege uvp USING project p WHERE uvp.project_id = p.project_id AND " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM project p WHERE " + test_sgid_condition, null);
    }

    public static void markVmDeleted(UUID vmId, DataSource dataSource) {
        Sql.with(dataSource).exec("UPDATE virtual_machine SET valid_until=now_utc() WHERE vm_id = ?", null, vmId);
    }

    public static void markSnapshotDestroyed(UUID snapshotId, DataSource dataSource) {
        Sql.with(dataSource).exec("UPDATE snapshot SET status=5 WHERE id = ?",  null, snapshotId);
    }

}
