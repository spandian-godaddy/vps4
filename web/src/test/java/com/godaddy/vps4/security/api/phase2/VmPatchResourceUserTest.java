package com.godaddy.vps4.security.api.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
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
    long hfsVmId = 98765;
    VirtualMachine virtualMachine;
    DataSource dataSource = injector.getInstance(DataSource.class);
    
    @Before
    public void setupTest(){
        injector.injectMembers(this);
        orionGuid = UUID.randomUUID();
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        virtualMachine = SqlTestData.insertTestVm(orionGuid, validUser.getId(), dataSource);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
    }
    
    @After
    public void teardownTest(){
        SqlTestData.cleanupTestVmAndRelatedData(virtualMachine.vmId, dataSource);
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
        
        newValidVmResource().updateVm(virtualMachine.vmId, vmPatch);
        try{
            newInvalidVmResource().updateVm(virtualMachine.vmId, vmPatch);
            Assert.fail();
        }
        catch (AuthorizationException e) {
            //do nothing
        }
    }
}
