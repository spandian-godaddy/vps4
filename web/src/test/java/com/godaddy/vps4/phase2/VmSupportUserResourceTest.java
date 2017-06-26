package com.godaddy.vps4.phase2;

import java.lang.reflect.Method;
import java.util.UUID;

import javax.inject.Inject;
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
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.ActionWithDetails;
import com.godaddy.vps4.web.vm.VmSupportUserResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import gdg.hfs.vhfs.vm.VmService;

public class VmSupportUserResourceTest {

    @Inject
    Vps4UserService userService;

    @Inject
    DataSource dataSource;

    private GDUser user;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    VmService vmService = Mockito.mock(VmService.class);
                    bind(VmService.class).toInstance(vmService);

                    CreditService creditService = Mockito.mock(CreditService.class);
                    bind(CreditService.class).toInstance(creditService);
                    // Command Service
                    CommandService commandService = Mockito.mock(CommandService.class);
                    CommandState commandState = new CommandState();
                    commandState.commandId = UUID.randomUUID();
                    Mockito.when(commandService.executeCommand(Mockito.any(CommandGroupSpec.class)))
                            .thenReturn(commandState);
                    bind(CommandService.class).toInstance(commandService);
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
        return injector.getInstance(VmSupportUserResource.class);
    }

    private VirtualMachine createTestVm() {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(GDUserMock.DEFAULT_SHOPPER);
        VirtualMachine vm = SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
        return vm;
    }

    @Test
    public void testAddAdminUserAdminOnly() throws NoSuchMethodException {
        Method method = VmSupportUserResource.class.getMethod("addAdminUser", UUID.class);
        Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
    }

    @Test
    public void testAddAdminUser() {
        VirtualMachine vm = createTestVm();

        ActionWithDetails action = getVmSupportUserResource().addAdminUser(vm.vmId);
        Assert.assertEquals(action.type, ActionType.ADD_ADMIN_USER);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNotNull(action.message);
        Assert.assertTrue(action.message.contains("Username"));
        Assert.assertTrue(action.message.contains("Password"));
    }

    @Test
    public void testRemoveAdminUserAdminOnly() throws NoSuchMethodException {
        Method method = VmSupportUserResource.class.getMethod("removeAdminUser", UUID.class, String.class);
        Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
    }

    @Test
    public void testRemoveAdminUser() {
        VirtualMachine vm = createTestVm();

        ActionWithDetails action = getVmSupportUserResource()
                .removeAdminUser(vm.vmId, "random-username");
        Assert.assertEquals(action.type, ActionType.DELETE_ADMIN_USER);
        Assert.assertNotNull(action.commandId);
        Assert.assertNotNull(action.orchestrationCommand);
        Assert.assertNull(action.message);

        // Test that class overrides toString
        Assert.assertTrue(action.toString().contains("ActionWithDetails"));
    }

}
