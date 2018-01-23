package com.godaddy.vps4.phase2;

import java.lang.reflect.Method;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.godaddy.vps4.vm.jdbc.JdbcActionService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.web.security.EmployeeOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmActionWithDetails;
import com.godaddy.vps4.web.vm.VmSupportUserResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

public class VmSupportUserResourceTest {

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    VmUserService vmUserService = Mockito.mock(VmUserService.class);

    private GDUser user;
    private VmUser supportUser;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new SnapshotModule(),
            new Phase2ExternalsModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(VmUserService.class).toInstance(vmUserService);
                    bind(ActionService.class).to(JdbcActionService.class);
                    bind(ImageService.class).to(JdbcImageService.class);
                    bind(PrivilegeService.class).to(JdbcPrivilegeService.class); // TODO break out to security module
                    bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
                    SchedulerWebService swServ = Mockito.mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class).toInstance(swServ);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });


    @Before
    public void setupTest() {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmSupportUserResource getVmSupportUserResource() {
        Mockito.when(vmUserService.getSupportUser(Mockito.any(UUID.class))).thenReturn(supportUser);
        return injector.getInstance(VmSupportUserResource.class);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    @Test
    public void testAddSupportUserEmployeeOnly() throws NoSuchMethodException {
        Method method = VmSupportUserResource.class.getMethod("addSupportUser", UUID.class);
        Assert.assertTrue(method.isAnnotationPresent(EmployeeOnly.class));
    }

    @Test
    public void testAddSupportUser() {
        VirtualMachine vm = createTestVm();
        supportUser = null;

        VmActionWithDetails action = getVmSupportUserResource().addSupportUser(vm.vmId);
        Assert.assertEquals(action.type, ActionType.ADD_SUPPORT_USER);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNotNull(action.message);
        Assert.assertTrue(action.message.contains("Username"));
        Assert.assertTrue(action.message.contains("Password"));
    }

    @Test
    public void testAddSecondSupportUser() {
        VirtualMachine vm = createTestVm();
        supportUser = new VmUser("random-username", UUID.randomUUID(), true, VmUserType.SUPPORT);

        VmActionWithDetails action = getVmSupportUserResource().addSupportUser(vm.vmId);
        Assert.assertEquals(action.type, ActionType.SET_PASSWORD);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNotNull(action.message);
        Assert.assertTrue(action.message.contains("Username"));
        Assert.assertTrue(action.message.contains("Password"));
    }

    @Test
    public void testRemoveSupportUserEmployeeOnly() throws NoSuchMethodException {
        Method method = VmSupportUserResource.class.getMethod("removeSupportUser", UUID.class);
        Assert.assertTrue(method.isAnnotationPresent(EmployeeOnly.class));
    }

    @Test
    public void testRemoveSupportUser() {
        VirtualMachine vm = createTestVm();
        supportUser = new VmUser("random-username", UUID.randomUUID(), true, VmUserType.SUPPORT);

        VmActionWithDetails action = getVmSupportUserResource().removeSupportUser(vm.vmId);
        Assert.assertEquals(action.type, ActionType.REMOVE_SUPPORT_USER);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNull(action.message);

        // Test that class overrides toString
        Assert.assertTrue(action.toString().contains("VmActionWithDetails"));
    }
}
