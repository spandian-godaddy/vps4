package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
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
import com.godaddy.vps4.vm.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVmActionService;


public class SqlTestData {
    public final static String TEST_VM_NAME = "testVirtualMachine";
    public final static String TEST_SNAPSHOT_NAME = "test-snapshot";
    public final static String TEST_VM_SGID = "vps4-testing-";
    public final static long hfsSnapshotId = 123;
    public final static UUID notificationId = UUID.randomUUID();
    public final static String nfImageId = "test-imageid";
    public final static String INITIATED_BY = "tester";

    public static long getNextHfsVmId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(hfs_vm_id) as hfs_vm_id FROM virtual_machine",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("hfs_vm_id"))) + 1;
    }

    public static long getNextHfsAddressId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(hfs_address_id) as hfs_address_id FROM ip_address",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("hfs_address_id"))) + 1;
    }

    public static Vps4User insertTestVps4User(DataSource dataSource) {
        Vps4UserService vps4UserService = new JdbcVps4UserService(dataSource);
        return vps4UserService.getOrCreateUserForShopper("validUserShopperId", "1", UUID.randomUUID());
    }

    public static void deleteTestVps4User(DataSource dataSource) {
        Sql.with(dataSource).exec("DELETE FROM vps4_user WHERE shopper_id = ?", null, "validUserShopperId");
    }

    public static VirtualMachine insertDedicatedTestVm(UUID orionGuid, long vps4UserId, DataSource dataSource) {
        return insertTestVm(orionGuid, vps4UserId, dataSource, "centos7_64", 140, 1);
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, long vps4UserId, DataSource dataSource){
        return insertTestVm(orionGuid, vps4UserId, dataSource, "hfs-centos-7");
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, long vps4UserId, DataSource dataSource, int dcId){
        return insertTestVm(orionGuid, vps4UserId, dataSource, "hfs-centos-7", 10, dcId);
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, long vps4UserId, DataSource dataSource, String imageName) {
        return insertTestVm(orionGuid, vps4UserId, dataSource, imageName, 10, 1);
    }

    public static VirtualMachine insertTestVm(UUID orionGuid, long vps4UserId, DataSource dataSource, String imageName, int tier, int dataCenterId) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        long hfsVmId = getNextHfsVmId(dataSource);
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4UserId, dataCenterId, TEST_VM_SGID, orionGuid,
                TEST_VM_NAME, tier, imageName);
        VirtualMachine virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        virtualMachineService.setBackupJobId(virtualMachine.vmId, UUID.randomUUID());
        return virtualMachineService.getVirtualMachine(virtualMachine.vmId);
    }

    public static IpAddress insertTestIp(long hfsAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType, DataSource dataSource){
        NetworkService networkService = new JdbcNetworkService(dataSource);
        return networkService.createIpAddress(hfsAddressId, vmId, ipAddress, ipAddressType);
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
        snapshotService.saveVmHvForSnapshotTracking(vmId, "test_" + vmId);
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
        Sql.with(dataSource).exec("DELETE FROM vm_hypervisor_snapshottracking vh USING virtual_machine v"
                                          + " WHERE vh.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM scheduled_job sj USING virtual_machine v"
                                          + " WHERE sj.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM snapshot s USING virtual_machine v WHERE s.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM monitoring_pf m USING virtual_machine v WHERE m.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM managed_server ms USING project p JOIN virtual_machine v USING (project_id) WHERE ms.vm_id = v.vm_id AND " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine v USING project p WHERE v.project_id = p.project_id AND " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM notification_filter nf WHERE nf.notification_id = '" + notificationId + "'", null);
        Sql.with(dataSource).exec("DELETE FROM notification_extended_details ned WHERE ned.notification_id =  '" + notificationId + "'", null);
        Sql.with(dataSource).exec("DELETE FROM notification ntf WHERE ntf.notification_id =  '" + notificationId + "'", null);
        Sql.with(dataSource).exec("DELETE FROM project p WHERE " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM vps4_user WHERE shopper_id = ?", null, "validUserShopperId");
    }

    public static void addImportedVM(UUID vmId, DataSource dataSource) {
        Sql.with(dataSource).exec("INSERT INTO imported_vm (vm_id) VALUES (?)", null, vmId);
    }

    public static void removeImportedVM(UUID vmId, DataSource dataSource) {
        Sql.with(dataSource).exec("DELETE FROM imported_vm WHERE vm_id = ?", null, vmId);
    }
}
