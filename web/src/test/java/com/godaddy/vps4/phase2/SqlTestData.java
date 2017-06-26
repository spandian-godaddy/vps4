package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.json.simple.JSONObject;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.jdbc.JdbcActionService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;


public class SqlTestData {
    public final static String TEST_VM_NAME = "testVirtualMachine";
    public final static String TEST_VM_SGID = "vps4-testing-";

    public static long getNextHfsVmId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(hfs_vm_id) as hfs_vm_id FROM virtual_machine",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("hfs_vm_id"))) + 1;
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
        return virtualMachine;
    }

    public static Action insertTestVmAction(UUID commandId, UUID vmId, ActionType actionType, DataSource dataSource) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);
        ActionService actionService = new JdbcActionService(dataSource);
        long actionId = actionService.createAction(vmId, actionType, new JSONObject().toJSONString(), vps4UserId);
        actionService.tagWithCommand(actionId, commandId);
        return actionService.getAction(actionId);
    }

    public static void invalidateTestVm(UUID vmId, DataSource dataSource) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        virtualMachineService.destroyVirtualMachine(vm.hfsVmId);
    }

    public static void cleanupSqlTestData(DataSource dataSource) {
        String test_vm_condition = "v.name='" + TEST_VM_NAME + "'";
        String test_sgid_condition = "p.vhfs_sgid like '" + TEST_VM_SGID + "%'";

        Sql.with(dataSource).exec("DELETE FROM ip_address i USING virtual_machine v WHERE i.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM vm_user u USING virtual_machine v WHERE u.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM vm_action a USING virtual_machine v WHERE a.vm_id = v.vm_id AND " + test_vm_condition, null);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine v USING project p WHERE v.project_id = p.project_id AND " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM user_project_privilege uvp USING project p WHERE uvp.project_id = p.project_id AND " + test_sgid_condition, null);
        Sql.with(dataSource).exec("DELETE FROM project p WHERE " + test_sgid_condition, null);
    }

    @Deprecated
    public static void cleanupTestVmAndRelatedData(UUID vmId, DataSource dataSource) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);

        Sql.with(dataSource).exec("DELETE FROM ip_address WHERE vm_id = ?", null, vm.vmId);
        Sql.with(dataSource).exec("DELETE FROM vm_user WHERE vm_id = ?", null, vm.vmId);
        Sql.with(dataSource).exec("DELETE FROM vm_action where vm_id = ?", null, vm.vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM user_project_privilege WHERE project_id = ?", null, vm.projectId);
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_id = ?", null, vm.projectId);
    }
}