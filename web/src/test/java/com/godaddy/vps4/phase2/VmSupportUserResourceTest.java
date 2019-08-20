package com.godaddy.vps4.phase2;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.*;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.vm.jdbc.JdbcDataCenterService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVmActionService;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmActionWithDetails;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmSupportUserResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VmSupportUserResourceTest {

    @Inject
    Vps4UserService userService;
    @Inject
    DataSource dataSource;

    private VmUserService vmUserService = Mockito.mock(VmUserService.class);
    private VmResource vmResource = Mockito.mock(VmResource.class);

    private VirtualMachine vm;
    private List<VmUser> supportUsers;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new CancelActionModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(VmResource.class).toInstance(vmResource);
                    bind(VmUserService.class).toInstance(vmUserService);
                    bind(ActionService.class).to(JdbcVmActionService.class);
                    bind(ImageService.class).to(JdbcImageService.class);
                    bind(PrivilegeService.class).to(JdbcPrivilegeService.class); // TODO break out to security module
                    bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                    bind(DataCenterService.class).to(JdbcDataCenterService.class);
                }
            });

    @Before
    public void setupTest() {
        injector.injectMembers(this);
        vm = createTestVm();
        supportUsers = new ArrayList<>();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmSupportUserResource getVmSupportUserResource() {
        Mockito.when(vmUserService.supportUserExists(Mockito.any(String.class), Mockito.any(UUID.class))).thenAnswer(args -> {
            String username = (String) args.getArguments()[0];
            return supportUsers.stream().anyMatch(u -> u.username.equals(username));
        });
        Mockito.when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        return injector.getInstance(VmSupportUserResource.class);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    // add support user tests

    @Test
    public void testAddSupportUserExpectedRoles() throws NoSuchMethodException {
        GDUser.Role[] expectedRoles = new GDUser.Role[] {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.HS_AGENT, GDUser.Role.SUSPEND_AUTH};
        Assert.assertArrayEquals(expectedRoles, VmSupportUserResource.class.getAnnotation(RequiresRole.class).roles());
    }

    @Test
    public void testAddSupportUsers() {
        VmActionWithDetails action = getVmSupportUserResource().addSupportUsers(vm.vmId);
        Assert.assertEquals(ActionType.ADD_SUPPORT_USER, action.type);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNotNull(action.message);
        Assert.assertTrue(action.message.contains("Username"));
        Assert.assertTrue(action.message.contains("Password"));

        Mockito.verify(vmResource, Mockito.times(1)).getVm(Mockito.any(UUID.class));
    }

    @Test
    public void testAddAdditionalSupportUsers() {
        for (int i = 0; i < 3; i++) {
            VmActionWithDetails action = getVmSupportUserResource().addSupportUsers(vm.vmId);
            Assert.assertEquals(ActionType.ADD_SUPPORT_USER, action.type);
            Assert.assertNotNull(action.commandId);
            Assert.assertNotNull(action.orchestrationCommand);
            Assert.assertNotNull(action.message);
            Assert.assertTrue(action.message.contains("Username"));
            Assert.assertTrue(action.message.contains("Password"));
        }

        Mockito.verify(vmResource, Mockito.times(3)).getVm(Mockito.any(UUID.class));
    }

    @Test
    public void testRemoveSupportUsers() {
        supportUsers.add(new VmUser("support_test", UUID.randomUUID(), true, VmUserType.SUPPORT));

        VmActionWithDetails action = getVmSupportUserResource().removeSupportUsers(vm.vmId, "support_test");
        Assert.assertEquals(ActionType.REMOVE_SUPPORT_USER, action.type);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNull(action.message);

        // Test that class overrides toString
        Assert.assertTrue(action.toString().contains("VmActionWithDetails"));
        Mockito.verify(vmResource, Mockito.times(1)).getVm(Mockito.any(UUID.class));
    }

    @Test(expected = NotFoundException.class)
    public void testRemoveSupportUser404() {
        supportUsers.add(new VmUser("support_test", UUID.randomUUID(), true, VmUserType.SUPPORT));

        getVmSupportUserResource().removeSupportUsers(vm.vmId, "support_404");
    }

    // change support user password tests

    @Test
    public void testChangePassword() {
        supportUsers.add(new VmUser("support-test", UUID.randomUUID(), true, VmUserType.SUPPORT));

        VmActionWithDetails action = getVmSupportUserResource().changeSupportUsersPassword(vm.vmId, "support-test");
        Assert.assertEquals(action.type, ActionType.SET_PASSWORD);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNotNull(action.message);
        Assert.assertTrue(action.message.contains("Username"));
        Assert.assertTrue(action.message.contains("Password"));

        Mockito.verify(vmResource, Mockito.times(1)).getVm(Mockito.any(UUID.class));
    }

    @Test(expected = NotFoundException.class)
    public void testChangePassword404() {
        supportUsers.add(new VmUser("support-test", UUID.randomUUID(), true, VmUserType.SUPPORT));

        getVmSupportUserResource().changeSupportUsersPassword(vm.vmId, "support-404");
    }
}
