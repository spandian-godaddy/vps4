package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.sysadmin.SysAdminResource;
import com.godaddy.vps4.web.sysadmin.SysAdminResource.SetHostnameRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class SetHostnameTest {
    
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
    
//    Commands commands;
    CommandService commandService;
//    SysAdminService adminService;

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
                    commandService = Mockito.mock(CommandService.class);
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
    long hfsVmId = 98765;
    VirtualMachine virtualMachine;
    String username = "fakeUser";

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        orionGuid = UUID.randomUUID();
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        virtualMachine = SqlTestData.insertTestVm(orionGuid, validUser.getId(), injector.getInstance(DataSource.class));
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        vmUserService.createUser(username, virtualMachine.vmId);
    }
    
    @After
    public void teardownTest(){
        DataSource dataSource = injector.getInstance(DataSource.class);
        SqlTestData.cleanupTestVmAndRelatedData(virtualMachine.vmId, dataSource);
    }
    
    private SysAdminResource getValidResource() {
        user = validUser;
        return injector.getInstance(SysAdminResource.class);
    }
    
    @Test
    public void testSetValidHostname(){
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";
        Action action = getValidResource().setHostname(virtualMachine.vmId, request);
        Assert.assertNotNull(action.commandId);
    }
    
    @Test(expected=Vps4Exception.class)
    public void testSetInvalidHostname(){
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "invalidHostname..tst";
        getValidResource().setHostname(virtualMachine.vmId, request);
    }
    
    
}
