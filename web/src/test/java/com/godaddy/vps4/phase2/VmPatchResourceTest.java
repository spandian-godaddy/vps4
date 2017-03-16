package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmPatchResource.VmPatch;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class VmPatchResourceTest {
    
    Injector injector = Guice.createInjector(new DatabaseModule());
    VirtualMachineService virtualMachineService;
    ProjectService projectService;
    PrivilegeService privilegeService = mock(PrivilegeService.class);
    ActionService actionService = mock(ActionService.class);
    UUID orionGuid;
    DataSource dataSource = injector.getInstance(DataSource.class);
    long virtualMachineRequestId;
    String initialName;
    VirtualMachine virtualMachine;
    
    @Before
    public void setupTest(){
        virtualMachineService = new JdbcVirtualMachineService(dataSource);
        projectService = new JdbcProjectService(dataSource);
        
        orionGuid = UUID.randomUUID();
        virtualMachine = SqlTestData.insertTestVm(orionGuid, dataSource);
        initialName = virtualMachine.name;
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
        verify(actionService, times(1)).completeAction(anyLong(), eq("{}"), eq("Name = "+newName));
    }

    private VirtualMachine updateVmName(String newName) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(virtualMachine.vmId);
        assertEquals(initialName, vm.name);
        Vps4User user = new Vps4User(1234, "FakeShopper");
        VmPatchResource patchResource = new VmPatchResource(virtualMachineService, user, privilegeService, actionService);
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

