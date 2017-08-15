package com.godaddy.vps4.orchestration.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.json.simple.JSONObject;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;


public class SqlTestData {
    public final static String TEST_SHOPPER_ID = "orchtestShopperId";
    public final static String TEST_PROJECT_NAME = "orchtestProject";
    public final static String TEST_VM_NAME = "orchtestVirtualMachine";
    public final static String TEST_SNAPSHOT_NAME = "orch-snapshot";
    public final static String TEST_SGID = "orch-vps4-testing-";

    public static Vps4User insertUser(Vps4UserService userService) {
        return userService.getOrCreateUserForShopper(TEST_SHOPPER_ID);
    }

    public static Project insertProject(ProjectService projectService, Vps4UserService userService) {
        long userId = userService.getUser(TEST_SHOPPER_ID).getId();
        return projectService.createProject(TEST_PROJECT_NAME, userId, TEST_SGID);
    }

    public static VirtualMachine insertVm(VirtualMachineService virtualMachineService, Vps4UserService userService) {
        long hfsVmId = 145L;
        UUID orionGuid = UUID.randomUUID();
        String imageName = "centos-7";
        long userId = userService.getUser(TEST_SHOPPER_ID).getId();

        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(
                userId, 1, TEST_SGID, orionGuid, TEST_VM_NAME, 10, 1, imageName);
        VirtualMachine virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        return virtualMachineService.getVirtualMachine(virtualMachine.vmId);
    }

    public static UUID insertSnapshot(SnapshotService snapshotService, UUID vmId, long projectId, SnapshotType snapshotType) {
        return snapshotService.createSnapshot(projectId, vmId, TEST_SNAPSHOT_NAME, snapshotType);
    }

    public static UUID insertSnapshotWithStatus(SnapshotService snapshotService, UUID vmId,
                                      long projectId, SnapshotStatus status, SnapshotType snapshotType) {
        UUID snapshotId = insertSnapshot(snapshotService, vmId, projectId, snapshotType);
        snapshotService.updateSnapshotStatus(snapshotId, status);
        snapshotService.updateHfsSnapshotId(snapshotId, 123456);
        return snapshotId;
    }

    public static long insertSnapshotAction(ActionService actionService, Vps4UserService userService, UUID snapshotId) {
        long userId = userService.getUser(TEST_SHOPPER_ID).getId();
        return actionService.createAction(
                snapshotId, ActionType.CREATE_SNAPSHOT, new JSONObject().toJSONString(), userId);
    }

    public static void cleanupSqlTestData(DataSource dataSource, Vps4UserService userService) {
        long userId = userService.getUser(TEST_SHOPPER_ID).getId();
        String test_vm_condition = "v.name='" + TEST_VM_NAME + "'";
        String test_snapshot_condition = "s.name='" + TEST_SNAPSHOT_NAME + "'";
        String test_sgid_condition = "p.vhfs_sgid like '" + TEST_SGID + "%'";
        String test_user_condition = "u.shopper_id = '" + TEST_SHOPPER_ID + "'";
        String test_privilege_condition = "p.vps4_user_id = " + userId + "";

        Sql.with(dataSource).exec(
                "DELETE FROM snapshot_action a USING snapshot s WHERE a.snapshot_id = s.id AND " + test_snapshot_condition,
                null);
        Sql.with(dataSource).exec(
                "DELETE FROM snapshot a USING virtual_machine v WHERE a.vm_id = v.vm_id AND " + test_vm_condition,
                null);
        Sql.with(dataSource).exec(
                "DELETE FROM virtual_machine v USING project p WHERE v.project_id = p.project_id AND " + test_sgid_condition,
                null);
        Sql.with(dataSource).exec(
                "DELETE FROM user_project_privilege p WHERE " + test_privilege_condition, null);
        Sql.with(dataSource).exec("DELETE FROM project p WHERE " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM vps4_user u WHERE " + test_user_condition, null);
    }
}