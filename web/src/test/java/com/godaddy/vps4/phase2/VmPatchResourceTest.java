package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmPatchResource.VmPatch;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class VmPatchResourceTest {
    
    Injector injector = Guice.createInjector(new DatabaseModule());
    VirtualMachineService virtualMachineService;
    ProjectService projectService;
    PrivilegeService privilegeService = Mockito.mock(PrivilegeService.class);
    UUID orionGuid;
    DataSource dataSource = injector.getInstance(DataSource.class);
    long virtualMachineRequestId;
    String initialName = "testServer";
    VirtualMachine virtualMachine;
    
    @Before
    public void setupTest(){
        virtualMachineService = new JdbcVirtualMachineService(dataSource);
        projectService = new JdbcProjectService(dataSource);
        
        orionGuid = UUID.randomUUID();
        int managedLevel = 0;
        virtualMachineService.createVirtualMachineCredit(orionGuid, "linux", "cpanel", 10, managedLevel, "testShopperId");
        ProvisionVirtualMachineParameters params = new ProvisionVirtualMachineParameters(1, 1, "vps4-testing-",
                orionGuid, initialName, 10, managedLevel, "centos-7");
        virtualMachine = virtualMachineService.provisionVirtualMachine(params);
        long hfsVmId = 1000 + Math.abs((new Random().nextLong())); // HFS usually creates this, so we're making it up
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
    }
    
    @After
    public void teardownTest(){
        SqlTestData.cleanupTestVmAndRelatedData(virtualMachine.vmId, dataSource);
    }
    
    
    private void testValidServerName(String newName){
        VirtualMachine vm = updateVmName(newName);
        assertEquals(newName, vm.name);
    }

    private VirtualMachine updateVmName(String newName) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(initialName, vm.name);
        VmPatchResource patchResource = new VmPatchResource(virtualMachineService, null, privilegeService);
        VmPatch vmPatch = new VmPatch();
        vmPatch.name = newName;
        patchResource.updateVm(vm.vmId, vmPatch);
        vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        return vm;
    }
    
    @Test
    public void testUpdateServerName(){
        testValidServerName("NewVmName");
    }
    
    @Test
    public void testPunctuationAllowed(){
        testValidServerName("PunctuationOkay!@#$%^&*()-=+\"'");
    }
    
    @Test
    public void testSpacesOkay(){
        testValidServerName("This VM Name Has Spaces");
    }
    
    @Test
    public void testNonAlphabetical(){
        testValidServerName("º∂å∑¬˚∆´");
    }
    
    @Test
    public void testEmptyName(){
        // When an empty string is passed there is no change.
        VirtualMachine vm = updateVmName(new String(""));
        assertEquals(initialName, vm.name);
    }
}

