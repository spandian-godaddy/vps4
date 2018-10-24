package com.godaddy.vps4.phase2.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionService.ActionListFilters;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.jdbc.JdbcVmActionService;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class ActionServiceTest {

    private ActionService actionService;
    private Injector injector = Guice.createInjector(new DatabaseModule());

    private UUID orionGuid = UUID.randomUUID();
    private DataSource dataSource;
    private VirtualMachine vm;
    private VirtualMachine vm1;
    private VirtualMachine vm2;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);
        actionService = new JdbcVmActionService(dataSource);
        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        vm1 = SqlTestData.insertTestVm(orionGuid, dataSource);
        vm2 = SqlTestData.insertTestVm(orionGuid, dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm1.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vm2.vmId, dataSource);
    }

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
        actionFilters.byVmId(vm.vmId);

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");

        actions = actionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 1, actions.results.size());
    }

    @Test
    public void testGetAllActions() {
        ActionListFilters actionFilters = new ActionListFilters();

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm1.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm2.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");

        actions = actionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 4, actions.results.size());
    }

    @Test
    public void testGetAllActionsByType() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byType(ActionType.CREATE_VM, ActionType.STOP_VM);

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm1.vmId, ActionType.STOP_VM, "{}", "tester");
        actionService.createAction(vm2.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm.vmId, ActionType.START_VM, "{}", "tester");

        actions = actionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 3, actions.results.size());
    }

    @Test
    public void testGetAllActionsByStatus() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byStatus(ActionStatus.NEW);

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm1.vmId, ActionType.STOP_VM, "{}", "tester");
        actionService.createAction(vm2.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm.vmId, ActionType.START_VM, "{}", "tester");

        actions = actionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 4, actions.results.size());
    }

    @Test
    public void testGetActionsByTypeForVmId() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byVmId(vm.vmId);
        actionFilters.byType(ActionType.CREATE_VM);

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        long numberOfExistingActions = getNumberOfExistingActions(actions);

        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm.vmId, ActionType.STOP_VM, "{}", "tester");
        actionService.createAction(vm.vmId, ActionType.START_VM, "{}", "tester");

        actions = actionService.getActionList(actionFilters);
        assertEquals(numberOfExistingActions + 1, actions.results.size());
    }

    @Test
    public void testGetActionsInDateRange() {
        Instant before = Instant.now().minus(Duration.ofMinutes(1));
        actionService.createAction(vm.vmId, ActionType.SET_HOSTNAME, "{}", "tester");
        Instant after = Instant.now().plus(Duration.ofMinutes(1));

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byVmId(vm.vmId);
        actionFilters.byDateRange(before, after);

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        actionFilters.byDateRange(before, null);
        actions = actionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        actionFilters.byDateRange(null, after);
        actions = actionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        // No actions in range, date range ends before action
        actionFilters.byDateRange(null, before);
        actions = actionService.getActionList(actionFilters);
        assertEquals(null, actions);

        // No actions in range, date range starts after action
        actionFilters.byDateRange(after, null);
        actions = actionService.getActionList(actionFilters);
        assertEquals(null, actions);
    }

    @Test
    public void testGetActionsByStatus() {
        actionService.createAction(vm.vmId, ActionType.SET_HOSTNAME, "{}", "tester");

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byVmId(vm.vmId);
        actionFilters.byStatus(ActionStatus.NEW);

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());

        Action action = actions.results.get(0);
        actionService.completeAction(action.id, null, null);

        actionFilters.byStatus(ActionStatus.COMPLETE);
        actions = actionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());
    }

    @Test
    public void testCompleteActionPopulatesCompletedColumn() {
        actionService.createAction(vm.vmId, ActionType.SET_HOSTNAME, "{}", "tester");

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byVmId(vm.vmId);

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        Action testAction = actions.results.get(0);
        assertNull(testAction.completed);

        actionService.completeAction(testAction.id, "{}", "");

        actions = actionService.getActionList(actionFilters);
        testAction = actions.results.get(0);
        assertNotNull(testAction.completed);
    }

    @Test
    public void testGetActionsWithLimitAndOffset() {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byVmId(vm.vmId);

        actionService.createAction(vm.vmId, ActionType.CREATE_VM, "{}", "tester");
        actionService.createAction(vm.vmId, ActionType.STOP_VM, "{}", "tester");
        actionService.createAction(vm.vmId, ActionType.START_VM, "{}", "tester");

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);
        assertEquals(3, actions.results.size());

        actionFilters.setLimit(2);
        actions = actionService.getActionList(actionFilters);
        assertEquals(2, actions.results.size());

        actionFilters.setOffset(2);
        actionFilters.toString();
        actions = actionService.getActionList(actionFilters);
        assertEquals(1, actions.results.size());
        assertEquals(ActionType.CREATE_VM, actions.results.get(0).type); // order by created desc
    }
}
