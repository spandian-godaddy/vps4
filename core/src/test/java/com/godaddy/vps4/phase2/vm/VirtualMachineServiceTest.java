package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineRequest;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class VirtualMachineServiceTest {

    VirtualMachineService virtualMachineService;
    ProjectService projectService;
    Injector injector = Guice.createInjector(new DatabaseModule());
    Project project;
    private UUID orionGuid = UUID.randomUUID();
    DataSource dataSource = injector.getInstance(DataSource.class);
    long vmId;

    @Before
    public void setupService() {

        virtualMachineService = new JdbcVirtualMachineService(dataSource);
        projectService = new JdbcProjectService(dataSource);

        vmId = Sql.with(dataSource).exec("SELECT max(vm_id) as vm_id FROM virtual_machine",
                Sql.nextOrNull(this::mapVmId)) + 1;

        project = projectService.createProject("testVirtualMachineServiceProject", 1, 1);
    }

    private long mapVmId(ResultSet rs) throws SQLException {
        if (!rs.isAfterLast()) {
            return rs.getLong("vm_id");
        }
        return 0;
    }

    @After
    public void cleanupService() {

        DataSource dataSource = injector.getInstance(DataSource.class);

        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine_request WHERE orion_guid = ?", null, orionGuid);
        Sql.with(dataSource).exec("DELETE FROM user_project_privilege WHERE project_id = ?", null, project.getProjectId());
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_id = ?", null, project.getProjectId());

    }

    @Test
    public void testService() {

        String os = "linux";
        String controlPanel = "cpanel";
        int tier = 10;
        int managedLevel = 0;

        virtualMachineService.createVirtualMachineRequest(orionGuid, os, controlPanel, tier, managedLevel);

        VirtualMachineRequest vmRequest = virtualMachineService.getVirtualMachineRequest(orionGuid);

        assertNotNull(vmRequest);
        assertEquals(orionGuid, vmRequest.orionGuid);
        assertEquals(os, vmRequest.operatingSystem);
        assertEquals(controlPanel, vmRequest.controlPanel);
        assertEquals(tier, vmRequest.tier);
        assertEquals(managedLevel, vmRequest.managedLevel);

        String name = "testServer";
        long imageId = 1;
        int specId = 1;

        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, name, project.getProjectId(), specId, managedLevel, imageId);

        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);

        assertNotNull(vm);
        assertEquals(vmId, vm.vmId);
        assertEquals(name, vm.name);
        assertEquals(project.getProjectId(), vm.projectId);
        assertEquals(specId, vm.spec.specId);

        // vms =
        // virtualMachineService.listVirtualMachines(project.getProjectId());
    }

}
