package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;

public class SqlTestData {

    public static long getNextId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(vm_id) as vm_id FROM virtual_machine",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("vm_id"))) + 1;
    }

    public static long insertTestVm(UUID orionGuid, long projectId, DataSource dataSource) {
        JdbcVirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
        long vmId = getNextId(dataSource);
        virtualMachineService.createVirtualMachineRequest(orionGuid, "linux", "none", 10, 0, "testShopperId");
        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, "networkTestVm", projectId, 1, 0, 1);
        return vmId;
    }

    public static void cleanupTestVmAndRelatedData(long vmId, UUID orionGuid, DataSource dataSource) {
        Sql.with(dataSource).exec("DELETE FROM ip_address WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM vm_user WHERE virtual_machine_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine_request WHERE orion_guid = ?", null, orionGuid);
    }

    public static void cleanupTestProject(long projectId, DataSource dataSource) {
        Sql.with(dataSource).exec("DELETE FROM user_project_privilege WHERE project_id = ?", null, projectId);
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_id = ?", null, projectId);
    }
}