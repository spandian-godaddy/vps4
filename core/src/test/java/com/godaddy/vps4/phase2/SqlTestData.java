package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;


public class SqlTestData {

    public static long getNextId(DataSource dataSource) {
        return Sql.with(dataSource).exec("SELECT max(vm_id) as vm_id FROM virtual_machine",
                Sql.nextOrNull(rs -> rs.isAfterLast() ? 0 : rs.getLong("vm_id"))) + 1;
    }

    public static long insertTestVm(UUID orionGuid, long projectId, DataSource dataSource) {
        NetworkService networkService = new JdbcNetworkService (dataSource);
        ImageService imageService = new JdbcImageService(dataSource);
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource, networkService, imageService);
        long vmId = getNextId(dataSource);
        virtualMachineService.createVirtualMachineRequest(orionGuid, "linux", "none", 10, 0, "TestUser");
        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, "networkTestVm", projectId, 1, 0, 1);
        return vmId;
    }

    public static void cleanupTestVmAndRelatedData(long vmId, DataSource dataSource) {
        NetworkService networkService = new JdbcNetworkService(dataSource);
        ImageService imageService = new JdbcImageService(dataSource);
        VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource, networkService, imageService);
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);

        Sql.with(dataSource).exec("DELETE FROM ip_address WHERE vm_id = ?", null, vm.id);
        Sql.with(dataSource).exec("DELETE FROM vm_user WHERE vm_id = ?", null, vm.id);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM orion_request WHERE orion_guid = ?", null, vm.orionGuid);
    }

    public static void cleanupTestProject(long projectId, DataSource dataSource) {
        Sql.with(dataSource).exec("DELETE FROM user_project_privilege WHERE project_id = ?", null, projectId);
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_id = ?", null, projectId);
    }

    public static Project createProject(DataSource dataSource) {
        JdbcProjectService projectService = new JdbcProjectService(dataSource);
        return projectService.createProject(UUID.randomUUID().toString(), 1, 1, "vps4-test-");
    }
}