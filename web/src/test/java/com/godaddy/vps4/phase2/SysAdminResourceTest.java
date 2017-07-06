package com.godaddy.vps4.phase2;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
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

public class SysAdminResourceTest {

    @Inject Vps4UserService userService;
    @Inject VmUserService vmUserService;
    @Inject DataSource dataSource;

    private GDUser user;
    private String username = "fakeUser";
    private VirtualMachine vm;
    private Vm hfsVm;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    // HFS Vm Service
                    hfsVm = new Vm();
                    hfsVm.status = "ACTIVE";
                    VmService vmService = Mockito.mock(VmService.class);
                    Mockito.when(vmService.getVm(Mockito.anyLong())).thenReturn(hfsVm);
                    bind(VmService.class).toInstance(vmService);

                    CreditService creditService = Mockito.mock(CreditService.class);
                    bind(CreditService.class).toInstance(creditService);

                    // CommandService
                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class))).thenReturn(commandState);
                    bind(CommandService.class).toInstance(commandService);
                }

                @Provides
                protected GDUser provideUser() {
                    return user;
                }
            });

    @Before
    public void setupTest(){
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        vm = createTestVm();
        vmUserService.createUser(username, vm.vmId);
    }

    @After
    public void teardownTest(){
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private SysAdminResource getSysAdminResource() {
        return injector.getInstance(SysAdminResource.class);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    // === setPassword Tests ===
    @Test
    public void testShopperSetPassword(){
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;

        getSysAdminResource().setPassword(vm.vmId, request);
    }

    @Test
    public void testAdminSetPassword(){
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;

        user = GDUserMock.createAdmin();
        getSysAdminResource().setPassword(vm.vmId, request);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperSetPassword() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;

        user = GDUserMock.createShopper("shopperX");
        getSysAdminResource().setPassword(vm.vmId, request);
    }

    @Test
    public void testSetPasswordUserNotFound() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = "userX";

        try {
            getSysAdminResource().setPassword(vm.vmId, request);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("VM_USER_NOT_FOUND", e.getId());
        }
    }

    @Test
    public void testDoubleSetPassword(){
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;

        getSysAdminResource().setPassword(vm.vmId, request);
        try{
            getSysAdminResource().setPassword(vm.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void testSetPasswordWhileStopped(){
        hfsVm.status = "STOPPED";
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;

        try {
            getSysAdminResource().setPassword(vm.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_STATUS", e.getId());
        }
    }

    // === setHostname Tests ===
    @Test
    public void testShopperSetHostname() {
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";

        getSysAdminResource().setHostname(vm.vmId, request);
    }

    @Test
    public void testAdminSetHostname() {
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";

        user = GDUserMock.createAdmin();
        getSysAdminResource().setHostname(vm.vmId, request);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperSetHostname() {
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";

        user = GDUserMock.createShopper("shopperX");
        getSysAdminResource().setHostname(vm.vmId, request);
    }

    @Test
    public void testSetHostnameInvalidHostname() {
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "www.sooo.not.valid";

        try {
            getSysAdminResource().setHostname(vm.vmId, request);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_HOSTNAME", e.getId());
        }
    }

    @Test
    public void testDoubleSetHostname(){
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";

        getSysAdminResource().setHostname(vm.vmId, request);
        try{
            getSysAdminResource().setHostname(vm.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void testSetHostnameWhileStopped(){
        hfsVm.status = "STOPPED";
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "newhostname.test.tst";

        try{
            getSysAdminResource().setHostname(vm.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_STATUS", e.getId());
        }
    }

    // === enableUserAdmin Tests ===
    @Test
    public void testShopperEnableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        getSysAdminResource().enableUserAdmin(vm.vmId, request);
    }

    @Test
    public void testAdminEnableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        user = GDUserMock.createAdmin();
        getSysAdminResource().enableUserAdmin(vm.vmId, request);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperEnableUserAdmin() {
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        user = GDUserMock.createShopper("shopperX");
        getSysAdminResource().enableUserAdmin(vm.vmId, request);
    }

    @Test
    public void testEnableUserAdminUserNotFound() {
        SetAdminRequest request = new SetAdminRequest();
        request.username = "userX";

        try {
            getSysAdminResource().enableUserAdmin(vm.vmId, request);
            Assert.fail();
        } catch (Vps4Exception e) {
            Assert.assertEquals("VM_USER_NOT_FOUND", e.getId());
        }
    }

    @Test
    public void testDoubleEnableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        getSysAdminResource().enableUserAdmin(vm.vmId, request);
        try{
            getSysAdminResource().enableUserAdmin(vm.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void testEnableUserAdminWhileStopped(){
        hfsVm.status = "STOPPED";
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        try {
            getSysAdminResource().enableUserAdmin(vm.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_STATUS", e.getId());
        }
    }

    // === disableUserAdmin Tests ===
    @Test
    public void testShopperDisableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        getSysAdminResource().disableUserAdmin(vm.vmId, request);
    }

    @Test
    public void testAdminDisableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        user = GDUserMock.createAdmin();
        getSysAdminResource().disableUserAdmin(vm.vmId, request);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperDisableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        user = GDUserMock.createShopper("shopperX");
        getSysAdminResource().disableUserAdmin(vm.vmId, request);
    }

    @Test
    public void testDoubleDisableUserAdmin(){
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        getSysAdminResource().disableUserAdmin(vm.vmId, request);
        try{
            getSysAdminResource().disableUserAdmin(vm.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("CONFLICTING_INCOMPLETE_ACTION", e.getId());
        }
    }

    @Test
    public void testDisableUserAdminWhileStopped(){
        hfsVm.status = "STOPPED";
        SetAdminRequest request = new SetAdminRequest();
        request.username = username;

        try {
            getSysAdminResource().disableUserAdmin(vm.vmId, request);
            Assert.fail();
        }
        catch (Vps4Exception e) {
            Assert.assertEquals("INVALID_STATUS", e.getId());
        }
    }

}
