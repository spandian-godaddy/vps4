package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.mock;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.panopta.PanoptaApiCustomerService;
import com.godaddy.vps4.panopta.PanoptaApiServerService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.util.Cryptography;
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

public class SysAdminResourceTest {

    @Inject Vps4UserService userService;
    @Inject VmUserService vmUserService;
    @Inject DataSource dataSource;

    Cryptography cryptography;

    private GDUser user;
    private String username = "fakeUser";
    private VirtualMachine vm;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                protected void configure() {
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(PanoptaApiCustomerService.class).toInstance(mock(PanoptaApiCustomerService.class));
                    bind(PanoptaApiServerService.class).toInstance(mock(PanoptaApiServerService.class));
                    bind(MailRelayService.class).toInstance(mock(MailRelayService.class));
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
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    // === setPassword Tests ===
    @Test
    public void testShopperSetPassword(){
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;
        request.password = "newPassword1!";

        getSysAdminResource().setPassword(vm.vmId, request);
    }

    @Test
    public void testAdminSetPassword(){
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.username = username;
        request.password = "newPassword1!";

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
        request.password = "newPassword1!";

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
        Phase2ExternalsModule.mockHfsVm("STOPPED");
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
    public void testSetHostnameDefaultFormat() {
        SetHostnameRequest request = new SetHostnameRequest();
        request.hostname = "ip-111-112-113-114.ip.secureserver.net";

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
        Phase2ExternalsModule.mockHfsVm("STOPPED");
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
        Phase2ExternalsModule.mockHfsVm("STOPPED");
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
        Phase2ExternalsModule.mockHfsVm("STOPPED");
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
