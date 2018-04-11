package com.godaddy.vps4.phase2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriInfo;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.web.vm.VmAction;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmActionWithDetails;
import com.godaddy.vps4.web.vm.VmActionResource;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

public class VmActionResourceTest {

    private GDUser user;
    private UriInfo uri = mock(UriInfo.class);
    private CommandService commandService = mock(CommandService.class);

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {

                @Override
                public void configure() {
                    bind(CommandService.class).toInstance(commandService);
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
            });

    private VmActionResource getVmActionResource() {
        return injector.getInstance(VmActionResource.class);
    }

    @Before
    public void setupTest() throws URISyntaxException {
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        when(uri.getAbsolutePath()).thenReturn(new URI("/vmid/actions"));
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private VirtualMachine createTestVm(String shopperId) {
        UUID orionGuid = UUID.randomUUID();
        Vps4User vps4User = userService.getOrCreateUserForShopper(shopperId, "1");
        return SqlTestData.insertTestVm(orionGuid, vps4User.getId(), dataSource);
    }

    private Action createTestVmAction(UUID vmId, ActionType actionType) {
        UUID commandId = UUID.randomUUID();
        return SqlTestData.insertTestVmAction(commandId, vmId, actionType, dataSource);
    }

    @Test
    public void testShopperGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action coreVMAction = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = coreVMAction.commandId;

        VmAction vmAction = getVmActionResource().getVmAction(vm.vmId, coreVMAction.id);
        Assert.assertEquals(expectedGuid, vmAction.commandId);
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createShopper("shopperX");
        getVmActionResource().getVmAction(vm.vmId, action.id);
    }

    @Test(expected=NotFoundException.class)
    public void testNoSuchAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        long actionId = 1234567L;

        getVmActionResource().getVmAction(vm.vmId, actionId);
    }

    @Test(expected=NotFoundException.class)
    public void testCannotGetVmActionOnDifferentVm() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createShopper("shopperX");
        VirtualMachine xvm = createTestVm(user.getShopperId());
        getVmActionResource().getVmAction(xvm.vmId, action.id);
    }

    @Test
    public void testEmployeeGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action coreVMAction = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = coreVMAction.commandId;

        user = GDUserMock.createEmployee();
        VmAction vmAction = getVmActionResource().getVmAction(vm.vmId, coreVMAction.id);
        Assert.assertEquals(expectedGuid, vmAction.commandId);
    }

    @Test
    public void testE2SGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action coreVMAction = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = coreVMAction.commandId;

        user = GDUserMock.createEmployee2Shopper();
        VmAction vmAction = getVmActionResource().getVmAction(vm.vmId, coreVMAction.id);
        Assert.assertEquals(expectedGuid, vmAction.commandId);
    }

    @Test
    public void testAdminGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action coreVMAction = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = coreVMAction.commandId;

        user = GDUserMock.createAdmin();
        VmAction vmAction = getVmActionResource().getVmAction(vm.vmId, coreVMAction.id);
        Assert.assertEquals(expectedGuid, vmAction.commandId);
    }


    @Test
    public void testShopperListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action1 = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        Action action2 = createTestVmAction(vm.vmId, ActionType.STOP_VM);

        List<VmAction> vmActions = getVmActionResource().getActions(vm.vmId, 10, 0, null, null, uri).results;
        Assert.assertEquals(2, vmActions.size());

        List<UUID> commandIds = vmActions.stream().map(a -> a.commandId).collect(Collectors.toList());
        Assert.assertTrue(commandIds.contains(action1.commandId));
        Assert.assertTrue(commandIds.contains(action2.commandId));
    }

    @Test(expected=AuthorizationException.class)
    public void testUnauthorizedShopperListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createShopper("shopperX");
        getVmActionResource().getActions(vm.vmId, 10, 0, null, null, uri);
    }

    @Test
    public void testNoActionsListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());

        List<VmAction> vmActions = getVmActionResource().getActions(vm.vmId, 10, 0, null, null, uri).results;
        Assert.assertTrue(vmActions.isEmpty());
    }

    @Test
    public void testAdminListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createAdmin();
        List<VmAction> vmActions = getVmActionResource().getActions(vm.vmId, 10, 0, null, null, uri).results;
        Assert.assertEquals(vmActions.get(0).commandId, action.commandId);
    }

    @Test
    public void testGetActionListWithStatusFilter() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        List<String> statusList = Arrays.asList("NEW");

        List<VmAction> vmActions = getVmActionResource().getActions(vm.vmId, 10, 0, statusList, null, uri).results;
        Assert.assertEquals(1, vmActions.size());

        statusList = Arrays.asList("COMPLETE");
        vmActions = getVmActionResource().getActions(vm.vmId, 10, 0, statusList, null, uri).results;
        Assert.assertTrue(vmActions.isEmpty());
    }

    @Test
    public void testGetActionListByActionType() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        createTestVmAction(vm.vmId, ActionType.START_VM);
        createTestVmAction(vm.vmId, ActionType.STOP_VM);

        List<VmAction> vmActions = getVmActionResource().getActions(vm.vmId, 10, 0, null, ActionType.CREATE_VM, uri).results;
        Assert.assertEquals(1, vmActions.size());
        Assert.assertEquals(ActionType.CREATE_VM, vmActions.get(0).type);
    }

    @Test
    public void testGetActionDetailsAdminOnly() {
        try {
            Method method = VmActionResource.class.getMethod("getVmActionWithDetails", UUID.class, long.class);
            Assert.assertTrue(method.isAnnotationPresent(AdminOnly.class));
        }
        catch(NoSuchMethodException ex) {
            Assert.fail();
        }
    }

    @Test
    public void testGetVmActionDetails() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        CommandState command = mock(CommandState.class);
        when(commandService.getCommand(action.commandId)).thenReturn(command);

        user = GDUserMock.createAdmin();
        VmActionWithDetails detailedAction = getVmActionResource()
                .getVmActionWithDetails(vm.vmId, action.id);
        Assert.assertEquals(detailedAction.commandId, action.commandId);
        Assert.assertEquals(detailedAction.orchestrationCommand, command);
    }
}
