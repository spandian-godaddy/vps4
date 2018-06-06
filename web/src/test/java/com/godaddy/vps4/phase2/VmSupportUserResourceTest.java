package com.godaddy.vps4.phase2;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.*;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.vm.jdbc.JdbcActionService;
import com.godaddy.vps4.vm.jdbc.JdbcDataCenterService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.web.security.StaffOnly;
import com.godaddy.vps4.web.vm.VmActionWithDetails;
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

    VmUserService vmUserService = Mockito.mock(VmUserService.class);

    private List<VmUser> supportUsers = new ArrayList<>();

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
                    bind(DataCenterService.class).to(JdbcDataCenterService.class);
                }
            });


    @Before
    public void setupTest() {
        injector.injectMembers(this);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VmSupportUserResource getVmSupportUserResource() {
        Mockito.when(vmUserService.getSupportUsers(Mockito.any(UUID.class))).thenReturn(supportUsers);
        return injector.getInstance(VmSupportUserResource.class);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER, "1");
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    @Test
    public void testAddSupportUserStaffOnly() throws NoSuchMethodException {
        Method method = VmSupportUserResource.class.getMethod("addSupportUser", UUID.class);
        Assert.assertTrue(method.isAnnotationPresent(StaffOnly.class));
    }

    @Test
    public void testAddSupportUsers() {
        VirtualMachine vm = createTestVm();

        VmActionWithDetails action = getVmSupportUserResource().addSupportUsers(vm.vmId);
        Assert.assertEquals(ActionType.ADD_SUPPORT_USER, action.type);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNotNull(action.message);
        Assert.assertTrue(action.message.contains("Username"));
        Assert.assertTrue(action.message.contains("Password"));
    }

    @Test
    public void testAddAdditionalSupportUsers() {
        VirtualMachine vm = createTestVm();

        for (int i = 0; i < 3; i++) {
            VmActionWithDetails action = getVmSupportUserResource().addSupportUsers(vm.vmId);
            Assert.assertEquals(ActionType.ADD_SUPPORT_USER, action.type);
            Assert.assertNotNull(action.commandId);
            Assert.assertNotNull(action.orchestrationCommand);
            Assert.assertNotNull(action.message);
            Assert.assertTrue(action.message.contains("Username"));
            Assert.assertTrue(action.message.contains("Password"));
        }
    }

    @Test
    public void testRemoveSupportUserStaffOnly() throws NoSuchMethodException {
        Method method = VmSupportUserResource.class.getMethod("removeSupportUsers", UUID.class, String.class);
        Assert.assertTrue(method.isAnnotationPresent(StaffOnly.class));
    }

    @Test
    public void testRemoveSupportUsers() {
        VirtualMachine vm = createTestVm();
        supportUsers = new ArrayList<>();
        supportUsers.add(new VmUser("support_test", UUID.randomUUID(), true, VmUserType.SUPPORT));

        VmActionWithDetails action = getVmSupportUserResource().removeSupportUsers(vm.vmId, "support_test");
        Assert.assertEquals(ActionType.REMOVE_SUPPORT_USER, action.type);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNull(action.message);

        // Test that class overrides toString
        Assert.assertTrue(action.toString().contains("VmActionWithDetails"));
    }

    @Test(expected = NotFoundException.class)
    public void testRemoveSupportUser404() {
        VirtualMachine vm = createTestVm();
        supportUsers = new ArrayList<>();
        supportUsers.add(new VmUser("support_test", UUID.randomUUID(), true, VmUserType.SUPPORT));

        getVmSupportUserResource().removeSupportUsers(vm.vmId, "support_404");
    }
}
