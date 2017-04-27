package com.godaddy.vps4.phase2.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.credit.jdbc.JdbcCreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class VirtualMachineServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    NetworkService networkService = new JdbcNetworkService(dataSource);
    ImageService imageService = new JdbcImageService(dataSource);
    VirtualMachineService virtualMachineService = new JdbcVirtualMachineService(dataSource);
    CreditService creditService = new JdbcCreditService(dataSource);
    ProjectService projectService = new JdbcProjectService(dataSource);
    private UUID orionGuid = UUID.randomUUID();
    List<VirtualMachine> virtualMachines;
    List<UUID> vmCredits;
    String os = "linux";
    String controlPanel = "cpanel";
    Vps4User vps4User = new Vps4User(1, "TestUser");
    int tier = 10;
    int managedLevel = 0;
    int monitoring = 0;

    @Before
    public void setup() {
        List<VirtualMachine> oldVms = virtualMachineService.getVirtualMachinesForUser(vps4User.getId());
        for (VirtualMachine oldVm : oldVms) {
            SqlTestData.cleanupTestVmAndRelatedData(oldVm.vmId, dataSource);
        }

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
    public void testHasCPanel() {
        creditService.createVirtualMachineCredit(orionGuid, "centos", "cpanel", tier, managedLevel, monitoring, vps4User.getShopperId());
        vmCredits.add(orionGuid);
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, "testServer", 10, 1, "centos-7-cPanel-11");

        virtualMachineService.provisionVirtualMachine(params);

        VirtualMachine vm = virtualMachineService.getVirtualMachinesForUser(vps4User.getId()).get(0);
        virtualMachines.add(vm);
        Assert.assertTrue(virtualMachineService.virtualMachineHasCpanel(vm.vmId));
    }

    @Test
    public void testHasPleskPanel() {
        creditService.createVirtualMachineCredit(orionGuid, "windows", "plesk", tier, managedLevel, monitoring, vps4User.getShopperId());
        vmCredits.add(orionGuid);
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, "testServer", 10, 1, "windows-2012r2-plesk-12.5");

        virtualMachineService.provisionVirtualMachine(params);
        VirtualMachine vm = virtualMachineService.getVirtualMachinesForUser(vps4User.getId()).get(0);
        virtualMachines.add(vm);
        Assert.assertTrue(virtualMachineService.virtualMachineHasPlesk(vm.vmId));
    }

    @Test
    public void testService() throws InterruptedException {
        creditService.createVirtualMachineCredit(orionGuid, os, controlPanel, tier, managedLevel, monitoring, vps4User.getShopperId());

        VirtualMachineCredit vmRequest = creditService.getVirtualMachineCredit(orionGuid);

        assertNotNull(vmRequest);
        assertEquals(orionGuid, vmRequest.orionGuid);
        assertEquals(os, vmRequest.operatingSystem);
        assertEquals(controlPanel, vmRequest.controlPanel);
        assertEquals(tier, vmRequest.tier);
        assertEquals(managedLevel, vmRequest.managedLevel);
        assertEquals(AccountStatus.ACTIVE, vmRequest.accountStatus);
        assertNull(vmRequest.provisionDate);

        String name = "testServer";
        int specId = 1;

        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                orionGuid, name, 10, 1, "centos-7");

        UUID vmId = virtualMachineService.provisionVirtualMachine(params).vmId;
        creditService.claimVirtualMachineCredit(orionGuid, 1, vmId);

        vmRequest = creditService.getVirtualMachineCredit(orionGuid);
        assertNotNull(vmRequest.provisionDate);

        List<VirtualMachine> vms = virtualMachineService.getVirtualMachinesForUser(vps4User.getId());
        assertEquals(1, vms.size());

        VirtualMachine vm = vms.get(0);

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
        assertEquals(AccountStatus.ACTIVE, vm.accountStatus);
    }

    @Test
    public void testGetVirtualMachines() {
        List<UUID> createdVms = new ArrayList<>();
        for(int i = 0; i < 2; i++) {
            createdVms.add(UUID.randomUUID());
            virtualMachines.add(SqlTestData.insertTestVm(createdVms.get(i), vps4User.getId(), dataSource));
            vmCredits.add(UUID.randomUUID());
        }

        List<VirtualMachine> vms = virtualMachineService.getVirtualMachinesForUser(vps4User.getId());
        List<UUID> vmGuids = vms.stream().map(vm -> vm.orionGuid).collect(Collectors.toList());
        for (UUID vm : createdVms)
            assertTrue(vmGuids.contains(vm));
        assertEquals(virtualMachines.size(), vms.size());
    }

    @Test
    public void testGetOrCreateCredit() throws InterruptedException {

        List<VirtualMachineCredit> credits = creditService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(credits.isEmpty());
        int numberOfTasks = 10;
        for (int i = 0; i < numberOfTasks; i++) {
                creditService.createCreditIfNoneExists(vps4User);
        }

        credits = creditService.getVirtualMachineCredits(vps4User.getShopperId());
        assertEquals(1, credits.size());
        UUID guidWithCredit = credits.get(0).orionGuid;
        vmCredits.add(guidWithCredit);

        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(vps4User.getId(), 1, "vps4-testing-",
                credits.get(0).orionGuid, "test", 10, 1, "centos-7");

        VirtualMachine virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        creditService.claimVirtualMachineCredit(guidWithCredit, 1, virtualMachine.vmId);
        virtualMachines.add(virtualMachine);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, 1);


        creditService.createCreditIfNoneExists(vps4User);
        credits = creditService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(credits.isEmpty());

        virtualMachineService.destroyVirtualMachine(1);
        creditService.unclaimVirtualMachineCredit(guidWithCredit);

        creditService.createCreditIfNoneExists(vps4User);
        credits = creditService.getVirtualMachineCredits(vps4User.getShopperId());
        assertTrue(!credits.isEmpty());
        vmCredits.add(credits.get(0).orionGuid);
    }

}
