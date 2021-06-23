package com.godaddy.vps4.phase2;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.GDUserMock;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.GDUser.Role;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmActionResource;
import com.godaddy.vps4.web.vm.VmActionWithDetails;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;

import gdg.hfs.orchestration.CommandGroupSpec;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandSpec;
import gdg.hfs.orchestration.CommandState;

public class VmActionResourceTest {

    private GDUser user;
    private UriInfo uri = mock(UriInfo.class);
    private CommandService commandService = mock(CommandService.class);
    private CommandState commandState = mock(CommandState.class);
    private List<String> emptyList = new ArrayList<>();

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject ActionService actionService;
    @Inject Map<ActionType, String> actionTypeToCancelCmdNameMap;

    @Captor private ArgumentCaptor<CommandGroupSpec> commandGroupSpecArgumentCaptor;

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new VmModule(),
            new AbstractModule() {
                @Override
                public void configure() {
                    bind(CommandService.class).toInstance(commandService);

                    MapBinder<ActionType, String> actionTypeToCancelCmdNameMapBinder
                            = MapBinder.newMapBinder(binder(), ActionType.class, String.class);
                    actionTypeToCancelCmdNameMapBinder.addBinding(ActionType.SET_HOSTNAME)
                            .toInstance("SetHostnameCancelCommand");
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
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        when(uri.getAbsolutePath()).thenReturn(new URI("/vmid/actions"));
        commandState.commandId = UUID.randomUUID();
        when(commandService.executeCommand(any(CommandGroupSpec.class))).thenReturn(commandState);
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

    private Action createNullCommandIdTestVmAction(UUID vmId, ActionType actionType) {
        UUID commandId = null;
        return SqlTestData.insertTestVmAction(commandId, vmId, actionType, dataSource);
    }

    @Test
    public void testShopperGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action coreVMAction = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = coreVMAction.commandId;

        VmAction vmAction = getVmActionResource().getVmAction(vm.vmId, coreVMAction.id);
        Assert.assertEquals(expectedGuid, vmAction.commandId);
        Assert.assertEquals(false, vmAction.isRequesterEmployee);
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
        Assert.assertEquals(true, vmAction.isRequesterEmployee);
    }

    @Test
    public void testE2SGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action coreVMAction = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = coreVMAction.commandId;

        user = GDUserMock.createEmployee2Shopper();
        VmAction vmAction = getVmActionResource().getVmAction(vm.vmId, coreVMAction.id);
        Assert.assertEquals(expectedGuid, vmAction.commandId);
        Assert.assertEquals(true, vmAction.isRequesterEmployee);
    }

    @Test
    public void testAdminGetVmAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action coreVMAction = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        UUID expectedGuid = coreVMAction.commandId;

        user = GDUserMock.createAdmin();
        VmAction vmAction = getVmActionResource().getVmAction(vm.vmId, coreVMAction.id);
        Assert.assertEquals(expectedGuid, vmAction.commandId);
        Assert.assertEquals(true, vmAction.isRequesterEmployee);
    }

    @Test
    public void testShopperListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action1 = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        Action action2 = createTestVmAction(vm.vmId, ActionType.STOP_VM);

        List<VmAction> vmActions = getVmActionResource().getVmActionList(vm.vmId, emptyList, emptyList, null, null, 10, 0, uri).results;
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
        getVmActionResource().getVmActionList(vm.vmId, emptyList, emptyList, null, null, 10, 0, uri);
    }

    @Test
    public void testNoActionsListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());

        List<VmAction> vmActions = getVmActionResource().getVmActionList(vm.vmId, emptyList, emptyList, null, null, 10, 0, uri).results;
        Assert.assertTrue(vmActions.isEmpty());
    }

    @Test
    public void testAdminListActions() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);

        user = GDUserMock.createAdmin();
        List<VmAction> vmActions = getVmActionResource().getVmActionList(vm.vmId, emptyList, emptyList, null, null, 10, 0, uri).results;
        Assert.assertEquals(vmActions.get(0).commandId, action.commandId);
    }

    @Test
    public void testGetActionListWithStatusFilter() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        List<String> statusList = Arrays.asList("COMPLETE");

        List<VmAction> vmActions = getVmActionResource().getVmActionList(vm.vmId, statusList, emptyList, null, null, 10, 0, uri).results;
        Assert.assertEquals(0, vmActions.size());

        actionService.completeAction(action.id, null, null);
        vmActions = getVmActionResource().getVmActionList(vm.vmId, statusList, emptyList, null, null, 10, 0, uri).results;
        Assert.assertEquals(1, vmActions.size());
    }

    @Test
    public void testGetActionListByActionType() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        createTestVmAction(vm.vmId, ActionType.START_VM);
        createTestVmAction(vm.vmId, ActionType.STOP_VM);
        List<String> typeList = Arrays.asList("CREATE_VM");

        List<VmAction> vmActions = getVmActionResource().getVmActionList(vm.vmId, emptyList, typeList, null, null, 10, 0, uri).results;
        Assert.assertEquals(1, vmActions.size());
        Assert.assertEquals(ActionType.CREATE_VM, vmActions.get(0).type);
    }

    @Test
    public void testGetActionDetailsRequiresAdmin() {
        try {
            Method method = VmActionResource.class.getMethod("getVmActionWithDetails", UUID.class, long.class);
            Assert.assertTrue(method.isAnnotationPresent(RequiresRole.class));
            Role[] expectedRoles = new Role[] {Role.ADMIN, Role.HS_AGENT, Role.HS_LEAD, Role.SUSPEND_AUTH};
            Assert.assertArrayEquals(expectedRoles, method.getAnnotation(RequiresRole.class).roles());
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

    @Test
    public void testGetVmActionDetailsNoCommand() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = SqlTestData.insertTestVmAction(null, vm.vmId, ActionType.REINSTATE, dataSource);

        user = GDUserMock.createAdmin();
        VmActionWithDetails detailedAction = getVmActionResource()
                .getVmActionWithDetails(vm.vmId, action.id);
        Assert.assertEquals(detailedAction.commandId, null);
        Assert.assertEquals(detailedAction.orchestrationCommand, null);
    }

    @Test
    public void testCancelVmActionCancelsCorrespondingCommand() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        VmActionResource actionResource = getVmActionResource();
        actionResource.cancelVmAction(vm.vmId, action.id);
        verify(commandService, times(1)).cancel(action.commandId);
    }

    @Test
    public void testQueuesNewCancelCommand() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        // we create an action of type SET_HOSTNAME because in the injector above (test setup) we provide a
        // cancel command to be run for SET_HOSTNAME only
        Action action = createTestVmAction(vm.vmId, ActionType.SET_HOSTNAME);
        VmActionResource actionResource = getVmActionResource();
        actionResource.cancelVmAction(vm.vmId, action.id);

        verify(commandService, times(1)).executeCommand(commandGroupSpecArgumentCaptor.capture());

        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        long actionId = (Long) commandSpec.request;

        Assert.assertEquals(actionTypeToCancelCmdNameMap.get(ActionType.SET_HOSTNAME), commandSpec.command);
        Assert.assertEquals(actionId, action.id);
    }

    @Test
    public void testDoesNotQueueCancelCommandWhenNotSpecified() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        VmActionResource actionResource = getVmActionResource();
        actionResource.cancelVmAction(vm.vmId, action.id);
        verify(commandService, times(0)).executeCommand(any(CommandGroupSpec.class));
    }

    @Test
    public void testMarksActionAsCancelled() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        VmActionResource actionResource = getVmActionResource();
        actionResource.cancelVmAction(vm.vmId, action.id);

        Action modifiedAction = actionService.getAction(action.id);
        Assert.assertEquals(modifiedAction.status, ActionStatus.CANCELLED);
    }

    @Test
    public void testAddsNoteToCancelledAction() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.CREATE_VM);
        VmActionResource actionResource = getVmActionResource();
        actionResource.cancelVmAction(vm.vmId, action.id);

        Action modifiedAction = actionService.getAction(action.id);
        String expectedNote = "Action cancelled via api by admin";
        Assert.assertEquals(expectedNote, modifiedAction.note);
    }

    @Test
    public void testAddsCancelCommandIdToNoteIfApplicable() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.SET_HOSTNAME);
        VmActionResource actionResource = getVmActionResource();
        actionResource.cancelVmAction(vm.vmId, action.id);

        Action modifiedAction = actionService.getAction(action.id);
        String expectedNote = String.format(
                "%s. Async cleanup queued: %s", "Action cancelled via api by admin", commandState.commandId.toString());
        Assert.assertEquals(expectedNote, modifiedAction.note);
    }

    @Test
    public void testOnlyOpenToAdmins() {
        VmActionResource actionResource = getVmActionResource();
        try {
            Method m = actionResource.getClass().getMethod("cancelVmAction", UUID.class, long.class);
            GDUser.Role[] expectedRoles = new GDUser.Role[] {GDUser.Role.ADMIN};
            Assert.assertArrayEquals(expectedRoles, m.getAnnotation(RequiresRole.class).roles());
        }
        catch (NoSuchMethodException e) {
            Assert.fail("Cancel action should only be available to an admin");
        }
    }

    @Test(expected = Vps4Exception.class)
    public void testNonCancellableActionThrowsAnException() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createTestVmAction(vm.vmId, ActionType.SET_HOSTNAME);
        // a completed command cant be cancelled
        actionService.completeAction(action.id, new JSONObject().toJSONString(), "");
        VmActionResource actionResource = getVmActionResource();
        actionResource.cancelVmAction(vm.vmId, action.id);
    }

    @Test
    public void testNullCommandIdDoesNotCancelCommands() {
        VirtualMachine vm = createTestVm(user.getShopperId());
        Action action = createNullCommandIdTestVmAction(vm.vmId, ActionType.SET_HOSTNAME);
        VmActionResource actionResource = getVmActionResource();
        Assert.assertNull(action.commandId);
        actionResource.cancelVmAction(vm.vmId, action.id);
        verify(commandService, times(0)).cancel(action.commandId);
    }
}
