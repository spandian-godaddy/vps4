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
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineCredit;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class VirtualMachineServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    NetworkService networkService = new JdbcNetworkService(dataSource);
    ImageService imageService = new JdbcImageService(dataSource);
    VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
    ProjectService projectService = new JdbcProjectService(dataSource);
    private UUID orionGuid = UUID.randomUUID();
    List<VirtualMachine> virtualMachines;
    List<UUID> vmCredits;
    String os = "linux";
    String controlPanel = "cpanel";
    Vps4User vps4User = new Vps4User(1, "TestUser");
    int tier = 10;
    int managedLevel = 0;

    @Before
    public void setup() {
        virtualMachines = new ArrayList<>();
        vmCredits = new ArrayList<>();
    }

    @After
    public void cleanup() {
        for (VirtualMachine vm : virtualMachines) {
            SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        }
        for (UUID request : vmCredits) {
            Sql.with(dataSource).exec("DELETE FROM credit WHERE orion_guid = ?", null, request);
        }
    }

    @Test
    public void testService() {
        virtualMachineService.createVirtualMachineCredit(orionGuid, os, controlPanel, tier, managedLevel, vps4User.getShopperId());

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

        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, name, 10, 1, "centos-7");
        VirtualMachine vm = virtualMachineService.provisionVirtualMachine(params);
        virtualMachines.add(vm);
        long hfsVmId = SqlTestData.getNextHfsVmId(dataSource);
        virtualMachineService.addHfsVmIdToVirtualMachine(vm.vmId, hfsVmId);

        vm = virtualMachineService.getVirtualMachine(vm.vmId);
        verifyVm(name, specId, hfsVmId, vm);

        vm = virtualMachineService.getVirtualMachine(hfsVmId);
        verifyVm(name, specId, hfsVmId, vm);

        vm = virtualMachineService.getVirtualMachineByOrionGuid(orionGuid);
        verifyVm(name, specId, hfsVmId, vm);

    }

    private void verifyVm(String name, int specId, long hfsVmId, VirtualMachine vm) {
        assertNotNull(vm);
        assertEquals(hfsVmId, vm.hfsVmId);
        assertEquals(name, vm.name);
        assertEquals(specId, vm.spec.specId);
        assertEquals("centos-7", vm.image.hfsName);
        assertEquals("CentOS 7", vm.image.imageName);
    }

    @Test
    public void testGetVirtualMachines() {
        List<UUID> createdVms = new ArrayList<>();
        for(int i = 0; i < 2; i++) {
            createdVms.add(UUID.randomUUID());
            virtualMachines.add(SqlTestData.insertTestVm(createdVms.get(i), dataSource));
            vmCredits.add(UUID.randomUUID());
            virtualMachineService.createVirtualMachineCredit(vmCredits.get(i), os, controlPanel, tier, managedLevel,
                    vps4User.getShopperId());
        }

        List<VirtualMachineCredit> requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());

        List<UUID> requestGuids = requests.stream().map(rs -> rs.orionGuid).collect(Collectors.toList());
        for (UUID credit : vmCredits)
            assertTrue(requestGuids.contains(credit));
        assertEquals(vmCredits.size(), requests.size());

        List<VirtualMachine> vms = virtualMachineService.getVirtualMachinesForUser(vps4User.getId());
        List<UUID> vmGuids = vms.stream().map(vm -> vm.orionGuid).collect(Collectors.toList());
        for (UUID vm : createdVms)
            assertTrue(vmGuids.contains(vm));
        assertEquals(virtualMachines.size(), vms.size());
    }

    @Test
    public void testGetOrCreateCredit() throws InterruptedException {

        List<VirtualMachineCredit> requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(requests.isEmpty());

        int numberOfTasks = 10;
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < numberOfTasks; i++) {
            tasks.add(() -> {
                virtualMachineService.createCreditIfNoneExists(vps4User);
                return null;
            });
        }
        ExecutorService executor = Executors.newFixedThreadPool(numberOfTasks);
        executor.invokeAll(tasks);

        requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());
        assertEquals(1, requests.size());
        vmCredits.add(requests.get(0).orionGuid);

        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                requests.get(0).orionGuid, "test", 10, 1, "centos-7");

        VirtualMachine virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        virtualMachines.add(virtualMachine);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, 1);


        virtualMachineService.createCreditIfNoneExists(vps4User);
        requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(requests.isEmpty());

        virtualMachineService.destroyVirtualMachine(1);

        virtualMachineService.createCreditIfNoneExists(vps4User);
        requests = virtualMachineService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(!requests.isEmpty());
        vmCredits.add(requests.get(0).orionGuid);
    }

}
