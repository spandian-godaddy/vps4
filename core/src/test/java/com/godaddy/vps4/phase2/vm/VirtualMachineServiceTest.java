package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.Vps4User;
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
    List<Project> projects;
    List<Long> vmIds;
    List<UUID> vmRequests;
    String os = "linux";
    String controlPanel = "cpanel";
    Vps4User vps4User = new Vps4User(1, "TestUser");
    int tier = 10;
    int managedLevel = 0;

    @Before
    public void setup() {
        vmIds = new ArrayList<>();
        projects = new ArrayList<>();
        vmRequests = new ArrayList<>();
    }

    @After
    public void cleanup() {
        for (long vmId : vmIds) {
            SqlTestData.cleanupTestVmAndRelatedData(vmId, dataSource);
        }
        for (UUID request : vmRequests) {
            Sql.with(dataSource).exec("DELETE FROM orion_request WHERE orion_guid = ?", null, request);
        }
        for (Project project : projects) {
            SqlTestData.cleanupTestProject(project.getProjectId(), dataSource);
        }
    }

    @Test
    public void testService() {
        projects.add(SqlTestData.createProject(dataSource));
        long vmId = SqlTestData.getNextId(dataSource);
        vmIds.add(vmId);

        virtualMachineService.createVirtualMachineRequest(orionGuid, os, controlPanel, tier, managedLevel, vps4User.getShopperId());

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

        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, name, projects.get(0).getProjectId(), specId, managedLevel, imageId);

        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);

        assertNotNull(vm);
        assertEquals(vmId, vm.vmId);
        assertEquals(name, vm.name);
        assertEquals(projects.get(0).getProjectId(), vm.projectId);
        assertEquals(specId, vm.spec.specId);
    }

    @Test
    public void testGetVirtualMachines() {
        List<UUID> createdVms = new ArrayList<>();
        for(int i = 0; i < 2; i++) {
            projects.add(SqlTestData.createProject(dataSource));
            createdVms.add(UUID.randomUUID());
            vmIds.add(SqlTestData.insertTestVm(createdVms.get(i), projects.get(i).getProjectId(), dataSource));
            vmRequests.add(UUID.randomUUID());
            virtualMachineService.createVirtualMachineRequest(vmRequests.get(i), os, controlPanel, tier, managedLevel,
                    vps4User.getShopperId());
        }

        List<UUID> requests = virtualMachineService.getOrionRequests(vps4User.getShopperId());
        for (UUID request : vmRequests)
            assertTrue(requests.contains(request));
        assertEquals(vmRequests.size(), requests.size());

        Map<UUID, String> vms = virtualMachineService.getVirtualMachines(vps4User.getId());
        for (UUID vm : createdVms)
            assertTrue(vms.containsKey(vm));
        assertEquals(vmIds.size(), vms.size());
    }
    
    @Test
    public void testGetOrCreateCredit() {
        
        List<UUID> requests = virtualMachineService.getOrionRequests(vps4User.getShopperId());
        assertTrue(requests.isEmpty());

        virtualMachineService.createOrionRequestIfNoneExists(vps4User);
        requests = virtualMachineService.getOrionRequests(vps4User.getShopperId());
        assertTrue(!requests.isEmpty());
        vmRequests.add(requests.get(0));

        Project project = SqlTestData.createProject(dataSource);
        virtualMachineService.provisionVirtualMachine(1, requests.get(0), "test", project.getProjectId(), 2, 0, 1);
        vmIds.add((long) 1);

        virtualMachineService.createOrionRequestIfNoneExists(vps4User);
        requests = virtualMachineService.getOrionRequests(vps4User.getShopperId());
        assertTrue(requests.isEmpty());

        virtualMachineService.destroyVirtualMachine(1);

        virtualMachineService.createOrionRequestIfNoneExists(vps4User);
        requests = virtualMachineService.getOrionRequests(vps4User.getShopperId());
        assertTrue(!requests.isEmpty());
        vmRequests.add(requests.get(0));
    }

}
