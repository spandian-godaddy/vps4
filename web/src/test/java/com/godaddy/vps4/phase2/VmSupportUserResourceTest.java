package com.godaddy.vps4.phase2;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.*;
import com.godaddy.vps4.security.jdbc.JdbcPrivilegeService;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.vm.jdbc.JdbcActionService;
import com.godaddy.vps4.vm.jdbc.JdbcImageService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmActionWithDetails;
import com.godaddy.vps4.web.vm.VmSupportUserResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.VmService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.UUID;

public class VmSupportUserResourceTest {

    @Inject
    Vps4UserService userService;

    @Inject
    DataSource dataSource;

    VmUserService vmUserService = Mockito.mock(VmUserService.class);

    private GDUser user;
    private VmUser supportUser;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    VmService vmService = Mockito.mock(VmService.class);
                    bind(VmService.class).toInstance(vmService);

                    CreditService creditService = Mockito.mock(CreditService.class);
                    bind(CreditService.class).toInstance(creditService);

                    bind(VmUserService.class).toInstance(vmUserService);

                    // Command Service
                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                            .thenReturn(commandState);
                    bind(CommandService.class).toInstance(commandService);

                    bind(ActionService.class).to(JdbcActionService.class);
                    bind(ImageService.class).to(JdbcImageService.class);
                    bind(PrivilegeService.class).to(JdbcPrivilegeService.class); // TODO break out to security module
                    bind(VirtualMachineService.class).to(JdbcVirtualMachineService.class);
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
    public void testAddSupportUserAdminOnly() throws NoSuchMethodException {
        Method method = VmSupportUserResource.class.getMethod("addSupportUser", UUID.class);
        Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
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
    public void testRemoveSupportUserAdminOnly() throws NoSuchMethodException {
        Method method = VmSupportUserResource.class.getMethod("removeSupportUser", UUID.class);
        Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
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
