package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.sysadmin.SysAdminResource;
import com.godaddy.vps4.web.sysadmin.SysAdminResource.SetAdminRequest;
import com.godaddy.vps4.web.sysadmin.SysAdminResource.UpdatePasswordRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class SysAdminResourceUserTest {
    
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

    @Inject
    VmUserService vmUserService;

    /*
    Module mockModule = binder -> {
        VmUserService vmUserService = Mockito.mock(VmUserService.class);
        binder.bind(VmUserService.class).toInstance(vmUserService);
    };
    */

    private Injector injector = Guice.createInjector(
            new SecurityModule(),
            new DatabaseModule(),
            new VmModule(),
            new AbstractModule() {
                
                @Override
                protected void configure() {
                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class))).thenReturn(commandState);
                    bind(CommandService.class).toInstance(commandService);
                }
                
                @Provides
                protected Vps4User provideUser() {
                    return user;
                }
            });
    
    Vps4User validUser;
    Vps4User invalidUser;
    Vps4User user;
    
    UUID orionGuid;
    long vmId = 98765;
    Project project;
    String username = "fakeUser";

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        orionGuid = UUID.randomUUID();
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        virtualMachineService.createVirtualMachineRequest(orionGuid, "linux", "cPanel", 10, 1, "validUserShopperId");
        project = projService.createProject("TestProject", validUser.getId(), 1, "vps4-test-");
        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, "fakeVM", project.getProjectId(), 1, 1, 1);
        vmUserService.createUser(username, vmId);
    }
    
    @After
    public void teardownTest(){
        DataSource dataSource = injector.getInstance(DataSource.class);
        Sql.with(dataSource).exec("DELETE FROM vm_user WHERE name = ? AND vm_id = ?", null, username, vmId);
        Sql.with(dataSource).exec("DELETE FROM vm_action where vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        projService.deleteProject(project.getProjectId());
    }
    
    private SysAdminResource getValidResource() {
        user = validUser;
        return injector.getInstance(SysAdminResource.class);
    }
    
    private SysAdminResource getInvalidResource() {
        user = invalidUser;
        return injector.getInstance(SysAdminResource.class);
    }
    
    @Test
    public void testSetPassword(){
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;
        
        getValidResource().setPassword(vmId, request);
        try{
            getInvalidResource().setPassword(vmId, request);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }
    
    @Test
    public void testEnableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;
        
        getValidResource().enableUserAdmin(vmId, request);
        try{
            getInvalidResource().enableUserAdmin(vmId, request);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }
    
    @Test
    public void testdisableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;
        
        getValidResource().disableUserAdmin(vmId, request);
        try{
            getInvalidResource().disableUserAdmin(vmId, request);
            Assert.fail();
        }catch (Vps4Exception e){
            //do nothing
        }
    }
    
    
}
