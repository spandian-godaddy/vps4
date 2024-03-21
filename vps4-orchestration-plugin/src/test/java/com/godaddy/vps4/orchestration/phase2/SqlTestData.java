package com.godaddy.vps4.orchestration.phase2;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
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
import com.godaddy.vps4.vm.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import org.json.simple.JSONObject;

import javax.sql.DataSource;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SqlTestData {
    public final static String TEST_SHOPPER_ID = "orchtestShopperId";
    public final static String TEST_PROJECT_NAME = "orchtestProject";
    public final static String TEST_VM_NAME = "orchtestVirtualMachine";
    public final static String TEST_SNAPSHOT_NAME = "orch-snapshot";
    public final static String TEST_SGID = "orch-vps4-testing-";
    public final static long hfsVmId = 145;
    public final static long hfsSnapshotId = 123;
    public final static String nfImageId = "nocfox-id";
    public final static String IMAGE_NAME = "hfs-centos-7";
    public final static String INITIATED_BY = "tester";
    public final static String TEST_USER_NAME = "fake_vm_user";
    public final static String TEST_CUSTOMER_NAME = "fake_vm_customer";

    public static Vps4User insertUser(Vps4UserService userService) {
        return userService.getOrCreateUserForShopper(TEST_SHOPPER_ID, "1", UUID.randomUUID());
    }

    public static Project insertProject(ProjectService projectService, Vps4UserService userService) {
        long userId = userService.getUser(TEST_SHOPPER_ID).getId();
        return projectService.createProject(TEST_PROJECT_NAME, userId, TEST_SGID);
    }

    public static VirtualMachine insertVm(VirtualMachineService virtualMachineService, Vps4UserService userService, int tier) {
        UUID orionGuid = UUID.randomUUID();
        String imageName = tier < 60 ? "hfs-centos-7" : "centos7_64";
        long userId = userService.getUser(TEST_SHOPPER_ID).getId();

        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(
                userId, 1, TEST_SGID, orionGuid, TEST_VM_NAME, tier, imageName);
        VirtualMachine virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        return virtualMachineService.getVirtualMachine(virtualMachine.vmId);
    }

    public static VirtualMachine insertVm(VirtualMachineService virtualMachineService, Vps4UserService userService) {
        int tier = 10; // default to using vm tier 10
        return insertVm(virtualMachineService, userService, tier);
    }

    public static VirtualMachine insertDedicatedVm(VirtualMachineService virtualMachineService, Vps4UserService userService) {
        int tier = 140; // use tier 140 for dedicated vm
        return insertVm(virtualMachineService, userService, tier);
    }

    public static long insertVmAction(ActionService actionService, UUID vmId, ActionType actionType) {
        return actionService.createAction(
                vmId, actionType, new JSONObject().toJSONString(), INITIATED_BY);
    }

    public static UUID insertSnapshot(SnapshotService snapshotService, UUID vmId, long projectId, SnapshotType snapshotType) {
        return snapshotService.createSnapshot(projectId, vmId, TEST_SNAPSHOT_NAME, snapshotType);
    }

    public static UUID insertSnapshotWithStatus(SnapshotService snapshotService, UUID vmId,
                                      long projectId, SnapshotStatus status, SnapshotType snapshotType) {
        UUID snapshotId = insertSnapshot(snapshotService, vmId, projectId, snapshotType);
        snapshotService.updateSnapshotStatus(snapshotId, status);
        snapshotService.updateHfsSnapshotId(snapshotId, hfsSnapshotId);
        snapshotService.updateHfsImageId(snapshotId, nfImageId);
        return snapshotId;
    }

    public static long insertSnapshotAction(ActionService actionService, Vps4UserService userService, UUID snapshotId) {
        return actionService.createAction(
                snapshotId, ActionType.CREATE_SNAPSHOT, new JSONObject().toJSONString(), INITIATED_BY);
    }

    private static String generateRandomIpAddress() {
        Random r = new Random();
        return String.format(
                "%d.%d.%d.%d", r.nextInt(256), r.nextInt(256),
                r.nextInt(256), r.nextInt(256));
    }

    private static long getRandomLong() {
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
    }

    public static List<IpAddress> insertIpAddresses(NetworkService networkService, UUID vmId,
                                         int count, IpAddress.IpAddressType addressType) {
        IntStream.range(0, count)
                .forEach(i -> networkService.createIpAddress(
                        getRandomLong(), vmId, generateRandomIpAddress(), addressType));
        return networkService.getVmIpAddresses(vmId)
                .stream()
                .filter(ia -> ia.ipAddressType.equals(addressType))
                .collect(Collectors.toList());
    }

    public static void insertVmUser(VmUser user, VmUserService vmUserService) {
        vmUserService.createUser(user.username, user.vmId, user.adminEnabled, user.vmUserType);
    }

    public static void cleanupSqlTestData(DataSource dataSource, Vps4UserService userService) {
        long userId = userService.getUser(TEST_SHOPPER_ID).getId();
        String test_vm_condition = "v.name='" + TEST_VM_NAME + "'";
        String test_snapshot_condition = "s.name='" + TEST_SNAPSHOT_NAME + "'";
        String test_sgid_condition = "p.vhfs_sgid like '" + TEST_SGID + "%'";
        String test_user_condition = "u.shopper_id = '" + TEST_SHOPPER_ID + "'";
        String test_privilege_condition = "p.vps4_user_id = " + userId + "";
        String test_vmUser_name_condition = "u.name = '" + TEST_USER_NAME + "'";
        String test_vmCustomer_name_condition = "u.name = '" + TEST_CUSTOMER_NAME + "'";

        Sql.with(dataSource).exec(
                "DELETE FROM snapshot_action a USING snapshot s WHERE a.snapshot_id = s.id AND " + test_snapshot_condition,
                null);
        Sql.with(dataSource).exec(
                "DELETE FROM snapshot a USING virtual_machine v WHERE a.vm_id = v.vm_id AND " + test_vm_condition,
                null);
        Sql.with(dataSource).exec(
                "DELETE FROM ip_address a USING virtual_machine v WHERE a.vm_id = v.vm_id AND " + test_vm_condition,
                null);
        Sql.with(dataSource).exec(
                "DELETE FROM vm_action a USING virtual_machine v WHERE a.vm_id = v.vm_id AND " + test_vm_condition,
                null);
        Sql.with(dataSource).exec("DELETE FROM vm_user u WHERE " + test_vmUser_name_condition, null);
        Sql.with(dataSource).exec("DELETE FROM vm_user u WHERE u.name = 'fake_user' ", null);
        Sql.with(dataSource).exec("DELETE FROM vm_user u WHERE " + test_vmCustomer_name_condition, null);
        Sql.with(dataSource).exec("DELETE FROM managed_server ms USING virtual_machine v WHERE ms.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec(
                "DELETE FROM virtual_machine v USING project p WHERE v.project_id = p.project_id AND " + test_sgid_condition,
                null);
        Sql.with(dataSource).exec("DELETE FROM project p WHERE " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM vps4_user u WHERE " + test_user_condition, null);
    }

}
