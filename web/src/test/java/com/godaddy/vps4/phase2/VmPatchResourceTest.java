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
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
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
    Project project;
    UUID orionGuid;
    UUID id;
    long vmId;
    DataSource dataSource = injector.getInstance(DataSource.class);
    long virtualMachineRequestId;
    String initialName = "testServer";
    
    @Before
    public void setupTest(){
        NetworkService networkService = new JdbcNetworkService(dataSource);
        ImageService imageService = new JdbcImageService(dataSource);
        virtualMachineService = new JdbcVirtualMachineService(dataSource, networkService, imageService);
        projectService = new JdbcProjectService(dataSource);
        project = projectService.createProject("testVirtualMachineServiceProject", 1, 1, "vps4-test-");
        
        orionGuid = UUID.randomUUID();
        int managedLevel = 0;
        virtualMachineService.createVirtualMachineRequest(orionGuid, "linux", "cpanel", 10, managedLevel, "testShopperId");
        
        vmId = 1000+Math.abs((new Random().nextLong()));  //HFS usually creates this, so we're making it up
        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, initialName, 
                                                     project.getProjectId(), 
                                                     1, managedLevel, 1);
    }
    
    @After
    public void teardownTest(){
        Sql.with(dataSource).exec("DELETE from virtual_machine where vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE from orion_request where orion_guid = ?", null, orionGuid);
        Sql.with(dataSource).exec("DELETE from user_project_privilege where project_id = ?", null, project.getProjectId());
        Sql.with(dataSource).exec("DELETE from project where project_id = ?", null, project.getProjectId());
    }
    
    
    private void testValidServerName(String newName){
        VirtualMachine vm = updateVmName(newName);
        assertEquals(newName, vm.name);
    }

    private VirtualMachine updateVmName(String newName) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        assertEquals(initialName, vm.name);
        VmPatchResource patchResource = new VmPatchResource(virtualMachineService, null, privilegeService);
        VmPatch vmPatch = new VmPatch();
        vmPatch.name = newName;
        patchResource.updateVm(vm.id, vmPatch);
        vm = virtualMachineService.getVirtualMachine(vmId);
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

