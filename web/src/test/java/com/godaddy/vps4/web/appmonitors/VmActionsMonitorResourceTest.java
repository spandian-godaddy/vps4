package com.godaddy.vps4.web.appmonitors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.MonitoringCheckpoint;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionService.ActionListFilters;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VmActionsMonitorResourceTest {

    private VmActionsMonitorResource vmActionsMonitorResource;
    private MonitorService monitorService = mock(MonitorService.class);
    private ActionService actionService = mock(ActionService.class);
    private List<VmActionData> expectedVmActionData;
    private List<SnapshotActionData> expectedSnapshotActionData;

    @Before
    public void setupTest() {
        expectedVmActionData = new ArrayList<>();
        VmActionData vmActionData = new VmActionData("fake-action-id-1", UUID.randomUUID(), UUID.randomUUID());
        expectedVmActionData.add(vmActionData);

        vmActionData = new VmActionData("fake-action-id-2", UUID.randomUUID(), UUID.randomUUID());
        expectedVmActionData.add(vmActionData);

        vmActionData = new VmActionData("fake-action-id-3", UUID.randomUUID(), UUID.randomUUID());
        expectedVmActionData.add(vmActionData);

        expectedSnapshotActionData = new ArrayList<>();

        SnapshotActionData snapshotActionData = new SnapshotActionData("fake-action-id-1", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        snapshotActionData = new SnapshotActionData("fake-action-id-2", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        snapshotActionData = new SnapshotActionData("fake-action-id-3", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        vmActionsMonitorResource = new VmActionsMonitorResource(monitorService, actionService);
    }

    @Test
    public void testGetProvisioningPendingVms() {
        when(monitorService.getVmsByActions(60L, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = vmActionsMonitorResource.getProvisioningPendingVms(60L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetStartPendingVms() {
        when(monitorService.getVmsByActions(15L, ActionType.START_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = vmActionsMonitorResource.getStartPendingVms(15L);

        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetStopPendingVms() {
        when(monitorService.getVmsByActions(15L, ActionType.STOP_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = vmActionsMonitorResource.getStopPendingVms(15L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    @Test
    public void testGetRestartPendingVms() {
        // setup the test to create a restart action in in-Progress status
        Action action = new Action(0, UUID.randomUUID(), ActionType.RESTART_VM, null, null, null,
                ActionStatus.IN_PROGRESS, Instant.now().minus(10, ChronoUnit.MINUTES), Instant.now(), null,
                UUID.randomUUID(), "tester");
        List<Action> testActions = new ArrayList<>();
        testActions.add(action);
        ResultSubset<Action> resultSubset = new ResultSubset<>(testActions, testActions.size());
        ArgumentMatcher<ActionListFilters> expectedActionFilters = new ArgumentMatcher<ActionListFilters>() {
            @Override
            public boolean matches(Object item) {
                ActionListFilters actionFilters = (ActionListFilters)item;
                return actionFilters.getTypeList().stream()
                        .anyMatch(f -> ActionType.valueOf(f.getActionTypeId()) == ActionType.RESTART_VM);
            }
        };

        when(actionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset);
        List<VmActionData> actualVmActionData = vmActionsMonitorResource.getRestartPendingVms(15L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(1, actualVmActionData.size());
    }

    @Test
    public void testGetRebootPendingVms() {
        // setup the test to create a restart action in in-Progress status
        Action action = new Action(0, UUID.randomUUID(), ActionType.POWER_CYCLE, null, null, null,
                ActionStatus.IN_PROGRESS, Instant.now().minus(10, ChronoUnit.MINUTES), Instant.now(), null,
                UUID.randomUUID(), "tester");
        List<Action> testActions = new ArrayList<>();
        testActions.add(action);
        ResultSubset<Action> resultSubset = new ResultSubset<>(testActions, testActions.size());
        ArgumentMatcher<ActionListFilters> expectedActionFilters = new ArgumentMatcher<ActionListFilters>() {
            @Override
            public boolean matches(Object item) {
                ActionListFilters actionFilters = (ActionListFilters) item;
                return actionFilters.getTypeList().stream()
                        .anyMatch(f -> ActionType.valueOf(f.getActionTypeId()) == ActionType.POWER_CYCLE);
            }
        };

        when(actionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset);
        List<VmActionData> actualVmActionData = vmActionsMonitorResource.getRestartPendingVms(15L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(1, actualVmActionData.size());
    }

    @Test
    public void testGetBackupPendingActions() {
        when(monitorService.getVmsBySnapshotActions(120L, ActionStatus.IN_PROGRESS, ActionStatus.NEW, ActionStatus.ERROR)).thenReturn(expectedSnapshotActionData);
        List<SnapshotActionData> actualSnapshotActionData = vmActionsMonitorResource.getBackupPendingActions(120L);
        Assert.assertNotNull(actualSnapshotActionData);
        Assert.assertEquals(expectedSnapshotActionData.size(), actualSnapshotActionData.size());
    }

    @Test
    public void testGetRestorePendingVms() {
        when(monitorService.getVmsByActions(120L, ActionType.RESTORE_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = vmActionsMonitorResource.getRestorePendingVms(120L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
    }

    private ResultSubset<Action> getTestResultSet(long size, ActionType actionType, ActionStatus actionStatus) {
        return getTestResultSet(size, actionType, actionStatus, new ArrayList<>());
    }

    private ResultSubset<Action> getTestResultSet(long size, ActionType actionType, ActionStatus actionStatus, List<Action> testActions) {
        Action action = new Action(0, UUID.randomUUID(), actionType, null, null, null,
                actionStatus, Instant.now().minus(10, ChronoUnit.MINUTES), Instant.now(), null,
                UUID.randomUUID(), "tester");
        testActions.add(action);

        UUID resourceId = UUID.randomUUID();
        for(long i = 1; i < size; i++) {
            Action action2 = new Action(i, resourceId, actionType, null, null, null,
                    actionStatus, Instant.now().minus(10, ChronoUnit.MINUTES), Instant.now(), null,
                    UUID.randomUUID(), "tester");
            testActions.add(action2);
        }
        return new ResultSubset<>(testActions, testActions.size());
    }

    @Test
    public void testCheckForFailingActions() {
        ResultSubset<Action> resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.COMPLETE);
        resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.ERROR, resultSubset.results);
        ResultSubset<Action> emptyResultSet = new ResultSubset<>(new ArrayList<>(), 0);

        ArgumentMatcher<ActionListFilters> expectedActionFilters = new ArgumentMatcher<ActionListFilters>() {
            @Override
            public boolean matches(Object item) {
                ActionListFilters actionFilters = (ActionListFilters)item;
                return actionFilters.getTypeList().get(0).equals(ActionType.START_VM);
            }
        };
        when(actionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset).thenReturn(emptyResultSet);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10);
        Assert.assertEquals(1, errorData.size());
        ActionTypeErrorData actionTypeErrorData = errorData.get(0);
        Assert.assertEquals(5, actionTypeErrorData.failedActions.size());
        Assert.assertEquals(ActionType.START_VM, actionTypeErrorData.actionType);
        Assert.assertTrue(actionTypeErrorData.failurePercentage == 50.0);
    }

    @Test
    public void testCheckForFailingActionsWithCheckpoint() {
        ResultSubset<Action> resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.COMPLETE);
        resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.ERROR, resultSubset.results);
        ResultSubset<Action> emptyResultSet = new ResultSubset<>(new ArrayList<>(), 0);
        MonitoringCheckpoint checkpoint = new MonitoringCheckpoint();
        checkpoint.checkpoint = Instant.now().minus(1, ChronoUnit.DAYS);

        when(monitorService.getMonitoringCheckpoint(ActionType.START_VM)).thenReturn(checkpoint);

        ArgumentMatcher<ActionListFilters> expectedActionFilters = new ArgumentMatcher<ActionListFilters>() {
            @Override
            public boolean matches(Object item) {
                ActionListFilters actionFilters = (ActionListFilters)item;
                return actionFilters.getTypeList().get(0).equals(ActionType.START_VM)
                        && actionFilters.getStart().equals(checkpoint.checkpoint);
            }
        };
        when(actionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset).thenReturn(emptyResultSet);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10);
        Assert.assertEquals(1, errorData.size());
        ActionTypeErrorData actionTypeErrorData = errorData.get(0);
        Assert.assertEquals(5, actionTypeErrorData.failedActions.size());
        Assert.assertEquals(ActionType.START_VM, actionTypeErrorData.actionType);
        Assert.assertTrue(actionTypeErrorData.failurePercentage == 50.0);
    }

    @Test
    public void testCheckForFailingActionsNotFullWindow() {
        ResultSubset<Action> resultSubset = getTestResultSet(2, ActionType.START_VM, ActionStatus.COMPLETE);
        resultSubset = getTestResultSet(3, ActionType.START_VM, ActionStatus.ERROR, resultSubset.results);
        ResultSubset<Action> emptyResultSet = new ResultSubset<>(new ArrayList<>(), 0);

        ArgumentMatcher<ActionListFilters> expectedActionFilters = new ArgumentMatcher<ActionListFilters>() {
            @Override
            public boolean matches(Object item) {
                ActionListFilters actionFilters = (ActionListFilters)item;
                return actionFilters.getTypeList().get(0).equals(ActionType.START_VM);
            }
        };
        when(actionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset).thenReturn(emptyResultSet);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10);
        Assert.assertEquals(1, errorData.size());
        ActionTypeErrorData actionTypeErrorData = errorData.get(0);
        Assert.assertEquals(3, actionTypeErrorData.failedActions.size());
        Assert.assertEquals(ActionType.START_VM, actionTypeErrorData.actionType);
        Assert.assertEquals(2, actionTypeErrorData.affectedAccounts);
        Assert.assertTrue("Expected 60%, actual " + actionTypeErrorData.failurePercentage, actionTypeErrorData.failurePercentage == 60.0);
    }

    @Test
    public void testCheckForFailingActionsEmpty() {
        when(actionService.getActionList(any(ActionListFilters.class))).thenReturn(null);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10);
        Assert.assertEquals(0, errorData.size());
    }

    @Test
    public void testGetCheckpoints() {
        MonitoringCheckpoint checkpoint = new MonitoringCheckpoint();
        checkpoint.checkpoint = Instant.now().minus(1, ChronoUnit.DAYS);
        checkpoint.actionType = ActionType.CREATE_VM;
        MonitoringCheckpoint checkpoint2 = new MonitoringCheckpoint();
        checkpoint2.checkpoint = Instant.now().minus(1, ChronoUnit.DAYS);
        checkpoint2.actionType = ActionType.STOP_VM;
        List<MonitoringCheckpoint> checkpoints = new ArrayList<>();
        checkpoints.add(checkpoint);
        checkpoints.add(checkpoint2);

        when(monitorService.getMonitoringCheckpoints()).thenReturn(checkpoints);

        List<MonitoringCheckpoint> actualCheckpoints = vmActionsMonitorResource.getMonitoringCheckpoints();

        Assert.assertEquals(2, actualCheckpoints.size());
    }

    @Test
    public void testGetCheckpoint() {
        MonitoringCheckpoint checkpoint = new MonitoringCheckpoint();
        checkpoint.checkpoint = Instant.now();
        checkpoint.actionType = ActionType.CREATE_VM;

        when(monitorService.getMonitoringCheckpoint(ActionType.CREATE_VM)).thenReturn(checkpoint);

        MonitoringCheckpoint actualCheckpoint = vmActionsMonitorResource.getMonitoringCheckpoint(ActionType.CREATE_VM);

        Assert.assertEquals(checkpoint.actionType, actualCheckpoint.actionType);
        Assert.assertEquals(checkpoint.checkpoint, actualCheckpoint.checkpoint);
    }

    @Test
    public void testDeleteCheckpoint() {
        doNothing().when(monitorService).deleteMonitoringCheckpoint(ActionType.CREATE_VM);

        vmActionsMonitorResource.deleteMonitoringCheckpoint(ActionType.CREATE_VM);

        verify(monitorService, times(1)).deleteMonitoringCheckpoint(ActionType.CREATE_VM);
    }

}