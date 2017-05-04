package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;


public class SqlTestData {

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
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4UserId, 1, "vps4-testing-", orionGuid,
                "testVirtualMachine", 10, 1, imageName);
        VirtualMachine virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        return virtualMachine;
    }

    public static void cleanupTestVmAndRelatedData(UUID vmId, DataSource dataSource) {
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);

        Sql.with(dataSource).exec("DELETE FROM ip_address WHERE vm_id = ?", null, vm.vmId);
        Sql.with(dataSource).exec("DELETE FROM vm_user WHERE vm_id = ?", null, vm.vmId);
        Sql.with(dataSource).exec("DELETE FROM vm_action where vm_id = ?", null, vm.vmId);
        Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, vm.orionGuid);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM user_project_privilege WHERE project_id = ?", null, vm.projectId);
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_id = ?", null, vm.projectId);
    }
}