package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
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

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
    ProjectService projectService = new JdbcProjectService(dataSource);
    private UUID orionGuid = UUID.randomUUID();
    Project project;
    long vmId;

    @Before
    public void setupService() {
        project = projectService.createProject("testVirtualMachineServiceProject", 1, 1);
        vmId = SqlTestData.getNextId(dataSource);
    }

    @After
    public void cleanupService() {
        SqlTestData.cleanupTestVmAndRelatedData(vmId, orionGuid, dataSource);
        SqlTestData.cleanupTestProject(project.getProjectId(), dataSource);
    }

    @Test
    public void testService() {

        String os = "linux";
        String controlPanel = "cpanel";
        String shopperId = "testShopperId";
        int tier = 10;
        int managedLevel = 0;

        virtualMachineService.createVirtualMachineRequest(orionGuid, os, controlPanel, tier, managedLevel, shopperId);

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
