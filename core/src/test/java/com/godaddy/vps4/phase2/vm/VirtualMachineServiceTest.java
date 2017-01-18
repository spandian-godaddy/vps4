package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class VirtualMachineServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    NetworkService networkService = new JdbcNetworkService(dataSource);
    ImageService imageService = new JdbcImageService(dataSource);
    VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource, networkService, imageService);
    ProjectService projectService = new JdbcProjectService(dataSource);
    private UUID orionGuid = UUID.randomUUID();
    List<Project> projects;
    List<UUID> vmIds;
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
        for (UUID vmId : vmIds) {
            SqlTestData.cleanupTestVmAndRelatedData(vmId, dataSource);
        }
        for (UUID request : vmRequests) {
            Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, request);
        }
        for (Project project : projects) {
            SqlTestData.cleanupTestProject(project.getProjectId(), dataSource);
        }
    }

    @Test
    public void testService() {
        projects.add(SqlTestData.createProject(dataSource));

        virtualMachineService.createVirtualMachineRequest(orionGuid, os, controlPanel, tier, managedLevel, vps4User.getShopperId());

        VirtualMachineCredit vmRequest = virtualMachineService.getVirtualMachineCredit(orionGuid);

        assertNotNull(vmRequest);
        assertEquals(orionGuid, vmRequest.orionGuid);
        assertEquals(os, vmRequest.operatingSystem);
        assertEquals(controlPanel, vmRequest.controlPanel);
        assertEquals(tier, vmRequest.tier);
        assertEquals(managedLevel, vmRequest.managedLevel);

        String name = "testServer";
        long imageId = 1;
        int specId = 1;

        UUID vmId = virtualMachineService.provisionVirtualMachine(orionGuid, name, projects.get(0).getProjectId(), specId, managedLevel, imageId);
        vmIds.add(vmId);
        long hfsVmId = SqlTestData.getNextHfsVmId(dataSource);
        virtualMachineService.addHfsVmIdToVirtualMachine(vmId, hfsVmId);

        VirtualMachine vm = virtualMachineService.getVirtualMachine(hfsVmId);
        verifyVm(name, specId, hfsVmId, vm);
        
        vm = virtualMachineService.getVirtualMachineByOrionGuid(orionGuid);
        verifyVm(name, specId, hfsVmId, vm);
        
        vm = virtualMachineService.getVirtualMachine(vmId);
        verifyVm(name, specId, hfsVmId, vm);
    }

    private void verifyVm(String name, int specId, long hfsVmId, VirtualMachine vm) {
        assertNotNull(vm);
        assertEquals(hfsVmId, vm.hfsVmId);
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

        List<VirtualMachineCredit> requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());

        List<UUID> requestGuids = requests.stream().map(rs -> rs.orionGuid).collect(Collectors.toList());
        for (UUID request : vmRequests)
            assertTrue(requestGuids.contains(request));
        assertEquals(vmRequests.size(), requests.size());

        List<VirtualMachine> vms = virtualMachineService.getVirtualMachinesForUser(vps4User.getId());
        List<UUID> vmGuids = vms.stream().map(vm -> vm.orionGuid).collect(Collectors.toList());
        for (UUID vm : createdVms)
            assertTrue(vmGuids.contains(vm));
        assertEquals(vmIds.size(), vms.size());
    }
    
    @Test
    public void testGetOrCreateCredit() throws InterruptedException {
        
        List<VirtualMachineCredit> requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(requests.isEmpty());

        int numberOfTasks = 10;
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(() -> {
                virtualMachineService.createOrionRequestIfNoneExists(vps4User);
                return null;
            });
        }
        ExecutorService executor = Executors.newFixedThreadPool(numberOfTasks);
        executor.invokeAll(tasks);

        requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());
        assertEquals(1, requests.size());
        vmRequests.add(requests.get(0).orionGuid);

        Project project = SqlTestData.createProject(dataSource);
        projects.add(project);
        UUID vmId = virtualMachineService.provisionVirtualMachine(requests.get(0).orionGuid, "test", project.getProjectId(), 2, 0, 1);
        vmIds.add(vmId);
        virtualMachineService.addHfsVmIdToVirtualMachine(vmId,1);
        

        virtualMachineService.createOrionRequestIfNoneExists(vps4User);
        requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(requests.isEmpty());

        virtualMachineService.destroyVirtualMachine(1);

        virtualMachineService.createOrionRequestIfNoneExists(vps4User);
        requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(!requests.isEmpty());
        vmRequests.add(requests.get(0).orionGuid);
    }

}
