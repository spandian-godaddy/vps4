package com.godaddy.vps4.phase2;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

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
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmSnapshotActionResource;
import com.godaddy.vps4.web.vm.VmSnapshotResource;
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


public class VmSnapshotActionResourceTest {

    private GDUser user;
    private final CommandService commandService = mock(CommandService.class);
    private final CommandState commandState = mock(CommandState.class);
    private final VmSnapshotResource vmSnapshotResource = mock(VmSnapshotResource.class);
    private VirtualMachine testVm;

    @Inject Vps4UserService userService;
    @Inject DataSource dataSource;
    @Inject SnapshotService snapshotService;
    @Inject @SnapshotActionService ActionService actionService;
    @Inject Map<ActionType, String> actionTypeToCancelCmdNameMap;

    @Captor private ArgumentCaptor<CommandGroupSpec> commandGroupSpecArgumentCaptor;

    private final Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new SecurityModule(),
            new SnapshotModule(),
            new AbstractModule() {
                @Override
                public void configure() {
                    bind(CommandService.class).toInstance(commandService);
                    bind(VmSnapshotResource.class).toInstance(vmSnapshotResource);

                    MapBinder<ActionType, String> actionTypeToCancelCmdNameMapBinder
                            = MapBinder.newMapBinder(binder(), ActionType.class, String.class);
                    actionTypeToCancelCmdNameMapBinder.addBinding(ActionType.CREATE_SNAPSHOT)
                            .toInstance("CreateSnapshotCancelCommand");
                }

                @Provides
                public GDUser provideUser() {
                    return user;
                }
    });

    private VmSnapshotActionResource getSnapshotActionResource() {
        return injector.getInstance(VmSnapshotActionResource.class);
    }

    @Before
    public void setupTest() {
        MockitoAnnotations.initMocks(this);
        injector.injectMembers(this);
        user = GDUserMock.createShopper();
        commandState.commandId = UUID.randomUUID();
        when(commandService.executeCommand(any(CommandGroupSpec.class))).thenReturn(commandState);
        when(vmSnapshotResource.getSnapshot(any(UUID.class), any(UUID.class))).thenReturn(null);
    }

    @After
    public void teardownTest() {
        SqlTestData.cleanupSqlTestData(dataSource);
    }

    private Snapshot createTestSnapshot(String shopperId) {
        Vps4User vps4User = userService.getOrCreateUserForShopper(shopperId, "1", UUID.randomUUID());
        testVm = SqlTestData.insertTestVm(UUID.randomUUID(), vps4User.getId(), dataSource);
        return SqlTestData.insertSnapshot(snapshotService, testVm.vmId, testVm.projectId, SnapshotType.ON_DEMAND);
    }

    private Action createTestSnapshotAction(UUID snapshotId, ActionType actionType) {
        UUID commandId = UUID.randomUUID();
        return SqlTestData.insertTestSnapshotAction(actionService, commandId, snapshotId, actionType, dataSource);
    }

    private Action createNullCommandIdTestSnapshotAction(UUID snapshotId, ActionType actionType) {
        UUID commandId = null;
        return SqlTestData.insertTestSnapshotAction(actionService, commandId, snapshotId, actionType, dataSource);
    }

    @Test
    public void testCancelSnapshotActionCancelsCorrespondingCommand() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createTestSnapshotAction(snapshot.id, ActionType.CREATE_SNAPSHOT);
        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        actionResource.cancelSnapshotAction(testVm.vmId, snapshot.id, action.id);
        verify(commandService, times(1)).cancel(action.commandId);
    }

    @Test
    public void testQueuesNewCancelCommand() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createTestSnapshotAction(snapshot.id, ActionType.CREATE_SNAPSHOT);
        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        actionResource.cancelSnapshotAction(testVm.vmId, snapshot.id, action.id);

        verify(commandService, times(1)).executeCommand(commandGroupSpecArgumentCaptor.capture());

        CommandGroupSpec commandGroupSpec = commandGroupSpecArgumentCaptor.getValue();
        CommandSpec commandSpec = commandGroupSpec.commands.get(0);
        long actionId = (Long) commandSpec.request;

        Assert.assertEquals(actionTypeToCancelCmdNameMap.get(ActionType.CREATE_SNAPSHOT), commandSpec.command);
        Assert.assertEquals(actionId, action.id);
    }

    @Test
    public void testDoesNotQueueCancelCommandWhenNotSpecified() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createTestSnapshotAction(snapshot.id, ActionType.DESTROY_SNAPSHOT);
        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        actionResource.cancelSnapshotAction(testVm.vmId, snapshot.id, action.id);
        verify(commandService, times(0)).executeCommand(any(CommandGroupSpec.class));
    }

    @Test
    public void testMarksActionAsCancelled() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createTestSnapshotAction(snapshot.id, ActionType.CREATE_SNAPSHOT);
        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        actionResource.cancelSnapshotAction(testVm.vmId, snapshot.id, action.id);

        Action modifiedAction = actionService.getAction(action.id);
        Assert.assertEquals(modifiedAction.status, ActionStatus.CANCELLED);
    }

    @Test
    public void testAddsNoteToCancelledAction() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createTestSnapshotAction(snapshot.id, ActionType.DESTROY_SNAPSHOT);
        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        actionResource.cancelSnapshotAction(testVm.vmId, snapshot.id, action.id);

        Action modifiedAction = actionService.getAction(action.id);
        String expectedNote = "Snapshot action cancelled via api by tester"; // username is 'tester'
        Assert.assertEquals(expectedNote, modifiedAction.note);
    }

    @Test
    public void testAddsCancelCommandIdToNoteIfApplicable() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createTestSnapshotAction(snapshot.id, ActionType.CREATE_SNAPSHOT);
        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        actionResource.cancelSnapshotAction(testVm.vmId, snapshot.id, action.id);

        Action modifiedAction = actionService.getAction(action.id);
        String expectedNote = String.format(
            "%s. Async cleanup queued: %s", "Snapshot action cancelled via api by tester",
            commandState.commandId.toString());
        Assert.assertEquals(expectedNote, modifiedAction.note);
    }

    @Test
    public void testOnlyOpenToAdmins() {
        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        try {
            Method m = actionResource.getClass()
                                     .getMethod("cancelSnapshotAction", UUID.class, UUID.class, long.class);
            GDUser.Role[] expectedRoles = new GDUser.Role[] {GDUser.Role.ADMIN};
            Assert.assertArrayEquals(expectedRoles, m.getAnnotation(RequiresRole.class).roles());
        }
        catch (NoSuchMethodException e) {
            Assert.fail("Cancel action should only be available to an admin");
        }
    }

    @Test(expected = Vps4Exception.class)
    public void testNonCancellableActionThrowsAnException() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createTestSnapshotAction(snapshot.id, ActionType.CREATE_SNAPSHOT);
        // a completed command can't be cancelled
        actionService.completeAction(action.id, new JSONObject().toJSONString(), "");

        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        actionResource.cancelSnapshotAction(testVm.vmId, snapshot.id, action.id);
    }

    @Test
    public void testNullCommandIdDoesNotCancelCommands() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createNullCommandIdTestSnapshotAction(snapshot.id, ActionType.CREATE_SNAPSHOT);
        VmSnapshotActionResource actionResource = getSnapshotActionResource();
        Assert.assertNull(action.commandId);
        actionResource.cancelSnapshotAction(testVm.vmId, snapshot.id, action.id);
        verify(commandService, times(0)).cancel(action.commandId);
    }

    @Test
    public void testShopperGetSnapshotActions() {
        Snapshot snapshot = createTestSnapshot(user.getShopperId());
        Action action = createTestSnapshotAction(snapshot.id, ActionType.CREATE_SNAPSHOT);
        List<SnapshotAction> actions = getSnapshotActionResource().getActions(testVm.vmId, snapshot.id);
        verify(vmSnapshotResource).getSnapshot(testVm.vmId, snapshot.id);
        Assert.assertEquals(1, actions.size());
        Assert.assertEquals(action.id, actions.get(0).id);
    }

}
