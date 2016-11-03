package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

    @Before
    public void setupService() {
        DataSource dataSource = injector.getInstance(DataSource.class);

        Sql.with(dataSource).exec("TRUNCATE TABLE virtual_machine CASCADE", null);
        Sql.with(dataSource).exec("TRUNCATE TABLE virtual_machine_request CASCADE", null);
        Sql.with(dataSource).exec("TRUNCATE TABLE project CASCADE", null);

        virtualMachineService = new JdbcVirtualMachineService(dataSource);
        projectService = new JdbcProjectService(dataSource);

        project = projectService.createProject("testVirtualMachineServiceProject", 1, 1);
    }

    @After
    public void cleanupService() {

        DataSource dataSource = injector.getInstance(DataSource.class);

        Sql.with(dataSource).exec("TRUNCATE TABLE virtual_machine CASCADE", null);
        Sql.with(dataSource).exec("TRUNCATE TABLE virtual_machine_request CASCADE", null);
        Sql.with(dataSource).exec("TRUNCATE TABLE project CASCADE", null);

    }

    @Test
    public void testService() {

        UUID orionGuid = java.util.UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");
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
        int vmId = 1;
        long imageId = 1;
        int specId = 1;

        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, name, project.getProjectId(), specId, managedLevel, imageId);

        VirtualMachine vm = virtualMachineService.getVirtualMachine(1);

        assertNotNull(vm);
        assertEquals(vmId, vm.vmId);
        assertEquals(name, vm.name);
        assertEquals(project.getProjectId(), vm.projectId);
        assertEquals(specId, vm.spec.specId);

        // vms =
        // virtualMachineService.listVirtualMachines(project.getProjectId());
    }

}
