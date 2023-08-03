package com.godaddy.vps4.phase2.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.vps4.security.Vps4User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.snapshot.jdbc.JdbcSnapshotActionService;
import com.godaddy.vps4.snapshot.jdbc.JdbcSnapshotService;
import com.godaddy.vps4.util.ActionListFilters;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.jdbc.JdbcVmActionService;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class ActionServiceTest {

    private ActionService vmActionService;
    private ActionService snapshotActionService;
    private SnapshotService snapshotService;
    private Injector injector = Guice.createInjector(new DatabaseModule());

    private UUID orionGuid = UUID.randomUUID();
    private DataSource dataSource;
    private VirtualMachine vm;
    private VirtualMachine vm1;
    private VirtualMachine vm2;
    private Vps4User vps4User;
    private UUID snapshotId;
    private UUID snapshotId1;
    private UUID snapshotId2;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);
        vmActionService = new JdbcVmActionService(dataSource);
        snapshotActionService = new JdbcSnapshotActionService(dataSource);
        snapshotService = new JdbcSnapshotService(dataSource);
        vps4User = SqlTestData.insertTestVps4User(dataSource);
        vm = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        vm1 = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        vm2 = SqlTestData.insertTestVm(orionGuid, dataSource, vps4User.getId());
        snapshotId = snapshotService.createSnapshot(vm.projectId, vm.vmId, "vmSnapshot", SnapshotType.AUTOMATIC);
        snapshotId1 = snapshotService.createSnapshot(vm1.projectId, vm1.vmId, "vm1Snapshot", SnapshotType.AUTOMATIC);
        snapshotId2 = snapshotService.createSnapshot(vm2.projectId, vm2.vmId, "vmSnapshot", SnapshotType.AUTOMATIC);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm1.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm2.vmId, dataSource);
        SqlTestData.deleteTestVps4User(dataSource);
    }


    /**** VM Actions ****/
    private long getNumberOfExistingActions(ResultSubset<Action> actions) {
        long numberOfExistingActions = 0;
        if (actions != null){
            numberOfExistingActions = actions.results.size();
        }
        return numberOfExistingActions;
    }

    @Test
    public void testGetAllActionsForVmId() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(vm.vmId);

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");

        actions = vmActionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 1, actions.results.size());
    }

    @Test
    public void testGetAllActions() {
        ActionListFilters actionFilters = new ActionListFilters();

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm1.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm2.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");

        actions = vmActionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 4, actions.results.size());
    }

    @Test
    public void testGetAllActionsByType() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byType(ActionType.CREATE_VM, ActionType.STOP_VM);

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm1.vmId, ActionType.STOP_VM, "{}", "tester");
        vmActionService.createAction(vm2.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.START_VM, "{}", "tester");

        actions = vmActionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 3, actions.results.size());
    }

    @Test
    public void testGetAllActionsByStatus() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byStatus(ActionStatus.NEW);

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm1.vmId, ActionType.STOP_VM, "{}", "tester");
        vmActionService.createAction(vm2.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.START_VM, "{}", "tester");

        actions = vmActionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 4, actions.results.size());
    }

    @Test
    public void testGetActionsByTypeForVmId() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(vm.vmId);
        actionFilters.byType(ActionType.CREATE_VM);

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.STOP_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.START_VM, "{}", "tester");

        actions = vmActionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 1, actions.results.size());
    }

    @Test
    public void testGetActionsInDateRange() {
        Instant before = Instant.now().minus(Duration.ofMinutes(1));
        vmActionService.createAction(vm.vmId, ActionType.SET_HOSTNAME, "{}", "tester");
        Instant after = Instant.now().plus(Duration.ofMinutes(1));

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(vm.vmId);
        actionFilters.byDateRange(before, after);

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        actionFilters.byDateRange(before, null);
        actions = vmActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        actionFilters.byDateRange(null, after);
        actions = vmActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        // No actions in range, date range ends before action
        actionFilters.byDateRange(null, before);
        actions = vmActionService.getActionList(actionFilters);
        assertEquals(null, actions);

        // No actions in range, date range starts after action
        actionFilters.byDateRange(after, null);
        actions = vmActionService.getActionList(actionFilters);
        assertEquals(null, actions);
    }

    @Test
    public void testGetActionsByStatus() {
        vmActionService.createAction(vm.vmId, ActionType.SET_HOSTNAME, "{}", "tester");

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(vm.vmId);
        actionFilters.byStatus(ActionStatus.NEW);

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        Action action = actions.results.get(0);
        vmActionService.completeAction(action.id, null, null);

        actionFilters.byStatus(ActionStatus.COMPLETE);
        actions = vmActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());
    }

    @Test
    public void testCompleteActionPopulatesCompletedColumn() {
        vmActionService.createAction(vm.vmId, ActionType.SET_HOSTNAME, "{}", "tester");

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(vm.vmId);

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        Action testAction = actions.results.get(0);
        assertNull(testAction.completed);

        vmActionService.completeAction(testAction.id, "{}", "");

        actions = vmActionService.getActionList(actionFilters);
        testAction = actions.results.get(0);
        assertNotNull(testAction.completed);
    }

    @Test
    public void testGetActionsWithLimitAndOffset() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(vm.vmId);

        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.STOP_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.START_VM, "{}", "tester");

        ResultSubset<Action> actions = vmActionService.getActionList(actionFilters);
        assertEquals(3, actions.results.size());

        actionFilters.setLimit(2);
        actions = vmActionService.getActionList(actionFilters);
        assertEquals(2, actions.results.size());

        actionFilters.setOffset(2);
        actionFilters.toString();
        actions = vmActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());
        assertEquals(ActionType.CREATE_VM, actions.results.get(0).type); // order by created desc
    }

    /**** Snapshot Actions ****/
    @Test
    public void testGetAllSnapshotActionsForSnapshotId() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(snapshotId);
        ResultSubset<Action> actions = snapshotActionService.getActionList(actionFilters);
        long existingActions = getNumberOfExistingActions(actions);

        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId, ActionType.DESTROY_SNAPSHOT, "{}", "tester");

        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(existingActions + 3, actions.results.size());
    }

    @Test
    public void testGetAllSnapshotActions() {
        ActionListFilters actionFilters = new ActionListFilters();

        ResultSubset<Action> actions = snapshotActionService.getActionList(actionFilters);
        long existingActions = getNumberOfExistingActions(actions);

        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId1, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId2, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId, ActionType.DESTROY_SNAPSHOT, "{}", "tester");

        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(existingActions + 4, actions.results.size());
    }

    @Test
    public void testGetAllSnapshotActionsByType() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byType(ActionType.CREATE_SNAPSHOT, ActionType.DESTROY_SNAPSHOT);

        ResultSubset<Action> actions = snapshotActionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId1, ActionType.STOP_VM, "{}", "tester");
        snapshotActionService.createAction(snapshotId2, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId, ActionType.DESTROY_SNAPSHOT, "{}", "tester");

        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 3, actions.results.size());
    }

    @Test
    public void testGetAllSnapshotActionsByStatus() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byStatus(ActionStatus.NEW);

        ResultSubset<Action> actions = snapshotActionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        snapshotActionService.createAction(snapshotId, ActionType.CREATE_VM, "{}", "tester");
        snapshotActionService.createAction(snapshotId1, ActionType.STOP_VM, "{}", "tester");
        snapshotActionService.createAction(snapshotId2, ActionType.CREATE_VM, "{}", "tester");
        snapshotActionService.createAction(snapshotId, ActionType.START_VM, "{}", "tester");

        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 4, actions.results.size());
    }

    @Test
    public void testGetSnapshotActionsByTypeForSnapshotId() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(snapshotId);
        actionFilters.byType(ActionType.CREATE_SNAPSHOT);

        ResultSubset<Action> actions = snapshotActionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId, ActionType.DESTROY_SNAPSHOT, "{}", "tester");

        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 1, actions.results.size());
    }

    @Test
    public void testGetSnapshotActionsInDateRange() {
        Instant before = Instant.now().minus(Duration.ofMinutes(1));
        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        Instant after = Instant.now().plus(Duration.ofMinutes(1));

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(snapshotId);
        actionFilters.byDateRange(before, after);

        ResultSubset<Action> actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        actionFilters.byDateRange(before, null);
        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        actionFilters.byDateRange(null, after);
        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        // No actions in range, date range ends before action
        actionFilters.byDateRange(null, before);
        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(null, actions);

        // No actions in range, date range starts after action
        actionFilters.byDateRange(after, null);
        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(null, actions);
    }

    @Test
    public void testGetSnapshotActionsByStatus() {
        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(snapshotId);
        actionFilters.byStatus(ActionStatus.NEW);

        ResultSubset<Action> actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        Action action = actions.results.get(0);
        snapshotActionService.completeAction(action.id, null, null);

        actionFilters.byStatus(ActionStatus.COMPLETE);
        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());
    }

    @Test
    public void testGetSnapshotActionsWithLimitAndOffset() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(snapshotId);

        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId, ActionType.DESTROY_SNAPSHOT, "{}", "tester");
        snapshotActionService.createAction(snapshotId, ActionType.CREATE_SNAPSHOT, "{}", "tester");

        ResultSubset<Action> actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(3, actions.results.size());

        actionFilters.setLimit(2);
        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(2, actions.results.size());

        actionFilters.setOffset(1);
        actions = snapshotActionService.getActionList(actionFilters);
        assertEquals(2, actions.results.size());
        assertEquals(ActionType.DESTROY_SNAPSHOT, actions.results.get(0).type); // order by created desc
    }

    @Test
    public void testGetVmActionTypesReturnsActionTypes() {
        List<String> expected = new ArrayList<>(Arrays.asList("CREATE_OH_BACKUP", "CREATE_VM", "DESTROY_OH_BACKUP"));
        vmActionService.createAction(vm.vmId, ActionType.DESTROY_OH_BACKUP, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.CREATE_OH_BACKUP, "{}", "tester");

        List<String> result = vmActionService.getVmActionTypes(vm.vmId);

        assertTrue(expected.equals(result));
    }

    @Test
    public void testGetVmActionTypesDoesNotReturnDuplicates() {
        List<String> expected = new ArrayList<>(Arrays.asList("CREATE_OH_BACKUP", "CREATE_VM", "DESTROY_OH_BACKUP"));
        vmActionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.CREATE_OH_BACKUP, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.DESTROY_OH_BACKUP, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.CREATE_OH_BACKUP, "{}", "tester");
        vmActionService.createAction(vm.vmId, ActionType.DESTROY_OH_BACKUP, "{}", "tester");

        List<String> result = vmActionService.getVmActionTypes(vm.vmId);

        assertTrue(expected.equals(result));
    }

    @Test
    public void testInsertAction() {
        Action originalAction = new Action(
                567,
                UUID.randomUUID(),
                ActionType.CREATE_VM,
                null,
                null,
                null,
                ActionStatus.COMPLETE,
                Instant.now(),
                Instant.now(),
                null,
                UUID.randomUUID(),
                "user");
        long newActionId = vmActionService.insertAction(vm.vmId, originalAction);

        ActionListFilters filters = new ActionListFilters();
        filters.byResourceId(vm.vmId);
        Action action = vmActionService.getAction(newActionId);

        assertEquals(vm.vmId, action.resourceId);
        assertEquals(ActionType.CREATE_VM, action.type);
        assertEquals(ActionStatus.COMPLETE, action.status);
        assertEquals("user", action.initiatedBy);
    }
}
