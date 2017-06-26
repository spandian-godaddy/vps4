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
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.sysadmin.SysAdminResource;
import com.godaddy.vps4.web.sysadmin.SysAdminResource.SetAdminRequest;
import com.godaddy.vps4.web.sysadmin.SysAdminResource.SetHostnameRequest;
import com.godaddy.vps4.web.sysadmin.SysAdminResource.UpdatePasswordRequest;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmService;

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
                    // HFS services
                    hfsVm = new Vm();
                    hfsVm.status = "ACTIVE";
                    hfsVm.vmId = hfsVmId;
                    VmService vmService = Mockito.mock(VmService.class);
                    Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);

                    // CommandService
                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class))).thenReturn(commandState);

                    bind(CommandService.class).toInstance(commandService);
                    bind(VmService.class).toInstance(vmService);
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
    String username = "fakeUser";
    VirtualMachine virtualMachine;
    DataSource dataSource = injector.getInstance(DataSource.class);

    Vm hfsVm;

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        orionGuid = UUID.randomUUID();
        validUser = userService.getOrCreateUserForShopper("validUserShopperId");
        invalidUser = userService.getOrCreateUserForShopper("invalidUserShopperId");
        virtualMachine = SqlTestData.insertTestVm(orionGuid, validUser.getId(), dataSource);
        virtualMachineService.addHfsVmIdToVirtualMachine(virtualMachine.vmId, hfsVmId);
        vmUserService.createUser(username, virtualMachine.vmId);
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupTestVmAndRelatedData(virtualMachine.vmId, dataSource);
    }

    private SysAdminResource getValidResource() {
        user = validUser;
        return injector.getInstance(SysAdminResource.class);
    }

    private SysAdminResource getInvalidResource() {
        user = invalidUser;
        return injector.getInstance(SysAdminResource.class);
    }

    @Test(expected=AuthorizationException.class)
    public void testSetPassword(){
        hfsVm.status = "ACTIVE";
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;

        getInvalidResource().setPassword(virtualMachine.vmId, request);
    }

    @Test(expected=AuthorizationException.class)
    public void testEnableUserAdmin(){
        hfsVm.status = "ACTIVE";
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        getInvalidResource().enableUserAdmin(virtualMachine.vmId, request);
    }

    @Test(expected=AuthorizationException.class)
    public void testDisableUserAdmin(){
        hfsVm.status = "ACTIVE";
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        getInvalidResource().disableUserAdmin(virtualMachine.vmId, request);
    }

    @Test(expected=AuthorizationException.class)
    public void testSetHostname(){
        hfsVm.status = "ACTIVE";
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";

        getInvalidResource().setHostname(virtualMachine.vmId, request);
    }

    @Test
    public void testDoubleSetPassword(){
        hfsVm.status = "ACTIVE";
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;

        Action action = getValidResource().setPassword(virtualMachine.vmId, request);
        try{
            getValidResource().setPassword(virtualMachine.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertNotNull(action.commandId);
        }
    }

    @Test
    public void testDoubleEnableUserAdmin(){
        hfsVm.status = "ACTIVE";
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        Action action = getValidResource().enableUserAdmin(virtualMachine.vmId, request);
        try{
            getValidResource().enableUserAdmin(virtualMachine.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertNotNull(action.commandId);
        }
    }

    @Test
    public void testDoubleDisableUserAdmin(){
        hfsVm.status = "ACTIVE";
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        Action action = getValidResource().disableUserAdmin(virtualMachine.vmId, request);
        try{
            getValidResource().disableUserAdmin(virtualMachine.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertNotNull(action.commandId);
        }
    }

    @Test
    public void testDoubleSetHostname(){
        hfsVm.status = "ACTIVE";
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";

        Action action = getValidResource().setHostname(virtualMachine.vmId, request);
        try{
            getValidResource().setHostname(virtualMachine.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertNotNull(action.commandId);
        }
    }

    @Test(expected=Vps4Exception.class)
    public void testSetPasswordWhileStopped(){
        hfsVm.status = "STOPPED";
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;

        getValidResource().setPassword(virtualMachine.vmId, request);
    }

    @Test(expected=Vps4Exception.class)
    public void testEnableUserAdminWhileStopped(){
        hfsVm.status = "STOPPED";
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        getValidResource().enableUserAdmin(virtualMachine.vmId, request);
    }

    @Test(expected=Vps4Exception.class)
    public void testDisableUserAdminWhileStopped(){
        hfsVm.status = "STOPPED";
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        getValidResource().disableUserAdmin(virtualMachine.vmId, request);
    }

    @Test(expected=Vps4Exception.class)
    public void testSetHostnameWhileStopped(){
        hfsVm.status = "STOPPED";
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";

        getValidResource().setHostname(virtualMachine.vmId, request);
    }
}
