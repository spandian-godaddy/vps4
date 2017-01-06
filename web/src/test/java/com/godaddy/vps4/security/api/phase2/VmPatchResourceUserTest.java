package com.godaddy.vps4.security.api.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.vm.VmPatchResource;
import com.godaddy.vps4.web.vm.VmPatchResource.VmPatch;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmPatchResourceUserTest {
    
    @Inject
    PrivilegeService privilegeService;
    
    @Inject
    VirtualMachineService virtualMachineService;
    
    @Inject
    ActionService actionService;
    
    @Inject
    Vps4UserService userService;
    
    @Inject
    ProjectService projService;

    private Injector injector = Guice.createInjector(new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {
                
                @Override
                protected void configure() {
                    // no extra configuration needed.
                    
                }
                @Provides
                public Vps4User provideUser() {
                    return user;
                }
            });
             
    Vps4User validUser;
    Vps4User invalidUser;
    Vps4User user;
    
    UUID orionGuid;
    long vmId = 98765;
    Project project;
    VirtualMachine vm;
    
    @Before
    public void setupTest(){
        injector.injectMembers(this);
        orionGuid = UUID.randomUUID();
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        virtualMachineService.createVirtualMachineRequest(orionGuid, "linux", "cPanel", 10, 1, "validUserShopperId");
        project = projService.createProject("TestProject", validUser.getId(), 1, "vps4-test-");
        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, "fakeVM", project.getProjectId(), 1, 1, 1);
        vm = virtualMachineService.getVirtualMachine(vmId);
    }
    
    @After
    public void teardownTest(){
        DataSource dataSource = injector.getInstance(DataSource.class);
        Sql.with(dataSource).exec("DELETE FROM vm_action where vm_id = ?", null, vm.id);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE id = ?", null, vm.id);
        projService.deleteProject(project.getProjectId());
    }
    
    protected VmPatchResource newValidVmResource() {
        user = validUser;
        return injector.getInstance(VmPatchResource.class);
    }

    protected VmPatchResource newInvalidVmResource() {
        user = invalidUser;
        return injector.getInstance(VmPatchResource.class);
    }

    @Test
    public void testSetPassword(){
        VmPatch vmPatch = new VmPatch();
        vmPatch.name = "fakeName";
        
        newValidVmResource().updateVm(vm.id, vmPatch);
        try{
            newInvalidVmResource().updateVm(vm.id, vmPatch);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }
}
