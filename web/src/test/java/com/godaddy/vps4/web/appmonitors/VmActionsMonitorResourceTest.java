package com.godaddy.vps4.web.appmonitors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import com.godaddy.vps4.appmonitors.Checkpoint;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.ActionCheckpoint;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.appmonitors.HvBlockingSnapshotsData;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.util.ActionListFilters;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

public class VmActionsMonitorResourceTest {

    private VmActionsMonitorResource vmActionsMonitorResource;
    private MonitorService monitorService = mock(MonitorService.class);
    private ActionService vmActionService = mock(ActionService.class);
    private VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    private List<SnapshotActionData> expectedSnapshotActionData;
    private long pendingThreshold = 60L;

    @Before
    public void setupTest() {
        expectedSnapshotActionData = new ArrayList<>();

        SnapshotActionData snapshotActionData = new SnapshotActionData("fake-action-id-1", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        snapshotActionData = new SnapshotActionData("fake-action-id-2", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        snapshotActionData = new SnapshotActionData("fake-action-id-3", UUID.randomUUID(), UUID.randomUUID(), ActionType.CREATE_SNAPSHOT.name(), ActionStatus.IN_PROGRESS.name(), Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        expectedSnapshotActionData.add(snapshotActionData);

        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.orionGuid = UUID.randomUUID();
        VirtualMachine virtualMachine2 = new VirtualMachine();
        virtualMachine2.orionGuid = UUID.randomUUID();
        when(virtualMachineService.getVirtualMachine(any())).thenReturn(virtualMachine).thenReturn(virtualMachine2);
        vmActionsMonitorResource = new VmActionsMonitorResource(monitorService, vmActionService, virtualMachineService);
    }

    private void validateActionFilters(List<ActionType> typeList, List<ActionStatus> statusList) {
        ArgumentCaptor<ActionListFilters> argument = ArgumentCaptor.forClass(ActionListFilters.class);
        verify(vmActionService).getActionList(argument.capture());
        ActionListFilters filters = argument.getValue();
        assertEquals(typeList, filters.getTypeList());
        assertEquals(statusList, filters.getStatusList());
        Instant expectedEndTime = Instant.now().minus(pendingThreshold - 1, ChronoUnit.MINUTES);
        assertTrue("EndDate later than expected", filters.getEnd().isBefore(expectedEndTime));
    }

    @Test
    public void filtersVmsForPendingProvisionEndpoint() {
        vmActionsMonitorResource.getProvisioningPendingVms(pendingThreshold);
        validateActionFilters(Arrays.asList(ActionType.CREATE_VM), Arrays.asList(ActionStatus.IN_PROGRESS));
    }

    @Test
    public void filtersVmsForPendingStartEndpoint() {
        vmActionsMonitorResource.getStartPendingVms(pendingThreshold);
        validateActionFilters(Arrays.asList(ActionType.START_VM), Arrays.asList(ActionStatus.IN_PROGRESS));
    }

    @Test
    public void filtersVmsForPendingStopEndpoint() {
        vmActionsMonitorResource.getStopPendingVms(pendingThreshold);
        validateActionFilters(Arrays.asList(ActionType.STOP_VM), Arrays.asList(ActionStatus.IN_PROGRESS));
    }

    @Test
    public void filtersVmsForPendingRestartEndpoint() {
        vmActionsMonitorResource.getRestartPendingVms(pendingThreshold);
        validateActionFilters(Arrays.asList(ActionType.RESTART_VM, ActionType.POWER_CYCLE),
                Arrays.asList(ActionStatus.IN_PROGRESS));
    }

    @Test
    public void filtersVmsForPendingRestoreEndpoint() {
        vmActionsMonitorResource.getRestorePendingVms(pendingThreshold);
        validateActionFilters(Arrays.asList(ActionType.RESTORE_VM), Arrays.asList(ActionStatus.IN_PROGRESS));
    }

    @Test
    public void filtersVmsForPendingNewActionsEndpoint() {
        vmActionsMonitorResource.getVmsWithPendingNewActions(pendingThreshold);
        validateActionFilters(Collections.emptyList(), Arrays.asList(ActionStatus.NEW));
    }

    @Test
    public void filtersVmsForPendingAllActionsEndpoint() {
        vmActionsMonitorResource.getVmsWithAllPendingActions(pendingThreshold);
        validateActionFilters(Collections.emptyList(), Arrays.asList(ActionStatus.IN_PROGRESS));
    }

    @Test
    public void verifyFieldsOfActionMappingToVmActionData() {
        UUID vmId = UUID.randomUUID();
        long hfsVmId = 33L;
        Action action = mock(Action.class);
        action.id = 23L;
        action.type = ActionType.CREATE_VM;
        action.resourceId = vmId;
        action.commandId = UUID.randomUUID();

        ResultSubset<Action> subset = new ResultSubset<>(Arrays.asList(action), 1);
        doReturn(subset).when(vmActionService).getActionList(any(ActionListFilters.class));
        doReturn(hfsVmId).when(virtualMachineService).getHfsVmIdByVmId(vmId);

        List<VmActionData> results = vmActionsMonitorResource.getProvisioningPendingVms(pendingThreshold);
        assertEquals(1, results.size());
        assertEquals(hfsVmId, results.get(0).hfsVmId);
        assertEquals(vmId, results.get(0).vmId);
    }

    @Test
    public void testGetBackupPendingActions() {
        when(monitorService.getVmsBySnapshotActions(120L, ActionStatus.IN_PROGRESS, ActionStatus.NEW, ActionStatus.ERROR)).thenReturn(expectedSnapshotActionData);
        List<SnapshotActionData> actualSnapshotActionData = vmActionsMonitorResource.getBackupPendingActions(120L);
        Assert.assertNotNull(actualSnapshotActionData);
        Assert.assertEquals(expectedSnapshotActionData.size(), actualSnapshotActionData.size());
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
        when(vmActionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset).thenReturn(emptyResultSet);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10, Collections.<String> emptyList());
        Assert.assertEquals(1, errorData.size());
        ActionTypeErrorData actionTypeErrorData = errorData.get(0);
        Assert.assertEquals(5, actionTypeErrorData.failedActions.size());
        Assert.assertEquals(ActionType.START_VM, actionTypeErrorData.actionType);
        Assert.assertEquals(false, actionTypeErrorData.isCritical);
        Assert.assertTrue(actionTypeErrorData.failurePercentage == 50.0);
    }

    @Test
    public void testCheckForFailingActionsWithCriticalActionType() {
        ResultSubset<Action> resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.COMPLETE);
        resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.ERROR, resultSubset.results);
        ResultSubset<Action> emptyResultSet = new ResultSubset<>(new ArrayList<>(), 0);
        List<String> criticalActionList = Arrays.asList("START_VM");

        ArgumentMatcher<ActionListFilters> expectedActionFilters = new ArgumentMatcher<ActionListFilters>() {
            @Override
            public boolean matches(Object item) {
                ActionListFilters actionFilters = (ActionListFilters)item;
                return actionFilters.getTypeList().get(0).equals(ActionType.START_VM);
            }
        };
        when(vmActionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset).thenReturn(emptyResultSet);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10, criticalActionList);
        Assert.assertEquals(1, errorData.size());
        ActionTypeErrorData actionTypeErrorData = errorData.get(0);
        Assert.assertEquals(5, actionTypeErrorData.failedActions.size());
        Assert.assertEquals(ActionType.START_VM, actionTypeErrorData.actionType);
        Assert.assertEquals(true, actionTypeErrorData.isCritical);
    }

    @Test
    public void testCheckForFailingActionsWithCheckpoint() {
        ResultSubset<Action> resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.COMPLETE);
        resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.ERROR, resultSubset.results);
        ResultSubset<Action> emptyResultSet = new ResultSubset<>(new ArrayList<>(), 0);
        ActionCheckpoint checkpoint = new ActionCheckpoint();
        checkpoint.checkpoint = Instant.now().minus(1, ChronoUnit.DAYS);

        when(monitorService.getActionCheckpoint(ActionType.START_VM)).thenReturn(checkpoint);

        ArgumentMatcher<ActionListFilters> expectedActionFilters = new ArgumentMatcher<ActionListFilters>() {
            @Override
            public boolean matches(Object item) {
                ActionListFilters actionFilters = (ActionListFilters)item;
                return actionFilters.getTypeList().get(0).equals(ActionType.START_VM)
                        && actionFilters.getStart().equals(checkpoint.checkpoint);
            }
        };
        when(vmActionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset).thenReturn(emptyResultSet);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10, Collections.<String> emptyList());
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
        when(vmActionService.getActionList(argThat(expectedActionFilters))).thenReturn(resultSubset).thenReturn(emptyResultSet);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10, Collections.<String> emptyList());
        Assert.assertEquals(1, errorData.size());
        ActionTypeErrorData actionTypeErrorData = errorData.get(0);
        Assert.assertEquals(3, actionTypeErrorData.failedActions.size());
        Assert.assertEquals(ActionType.START_VM, actionTypeErrorData.actionType);
        Assert.assertEquals(2, actionTypeErrorData.affectedAccounts);
        Assert.assertTrue("Expected 30, actual " + actionTypeErrorData.failurePercentage, actionTypeErrorData.failurePercentage == 30.0);
    }

    @Test
    public void testCheckForFailingActionsEmpty() {
        when(vmActionService.getActionList(any(ActionListFilters.class))).thenReturn(null);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10, Collections.<String> emptyList());
        Assert.assertEquals(0, errorData.size());
    }

    @Test
    public void testGetActionCheckpoints() {
        ActionCheckpoint checkpoint = new ActionCheckpoint();
        checkpoint.checkpoint = Instant.now().minus(1, ChronoUnit.DAYS);
        checkpoint.actionType = ActionType.CREATE_VM;
        ActionCheckpoint checkpoint2 = new ActionCheckpoint();
        checkpoint2.checkpoint = Instant.now().minus(1, ChronoUnit.DAYS);
        checkpoint2.actionType = ActionType.STOP_VM;
        List<ActionCheckpoint> checkpoints = new ArrayList<>();
        checkpoints.add(checkpoint);
        checkpoints.add(checkpoint2);

        when(monitorService.getActionCheckpoints()).thenReturn(checkpoints);

        List<ActionCheckpoint> actualCheckpoints = vmActionsMonitorResource.getActionCheckpoints();

        Assert.assertEquals(2, actualCheckpoints.size());
    }

    @Test
    public void testGetActionCheckpoint() {
        ActionCheckpoint checkpoint = new ActionCheckpoint();
        checkpoint.checkpoint = Instant.now();
        checkpoint.actionType = ActionType.CREATE_VM;

        when(monitorService.getActionCheckpoint(ActionType.CREATE_VM)).thenReturn(checkpoint);

        ActionCheckpoint actualCheckpoint = vmActionsMonitorResource.getActionCheckpoint(ActionType.CREATE_VM);

        Assert.assertEquals(checkpoint.actionType, actualCheckpoint.actionType);
        Assert.assertEquals(checkpoint.checkpoint, actualCheckpoint.checkpoint);
    }

    @Test
    public void testDeleteActionCheckpoint() {
        doNothing().when(monitorService).deleteActionCheckpoint(ActionType.CREATE_VM);

        vmActionsMonitorResource.deleteActionCheckpoint(ActionType.CREATE_VM);

        verify(monitorService, times(1)).deleteActionCheckpoint(ActionType.CREATE_VM);
    }

    @Test
    public void testGetCheckpoints() {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.checkpoint = Instant.now().minus(1, ChronoUnit.DAYS);
        checkpoint.name = Checkpoint.Name.CREATES_WITHOUT_PANOPTA;
        List<Checkpoint> checkpoints = Collections.singletonList(checkpoint);

        when(monitorService.getCheckpoints()).thenReturn(checkpoints);

        List<Checkpoint> actualCheckpoints = vmActionsMonitorResource.getCheckpoints();

        Assert.assertEquals(1, actualCheckpoints.size());
    }

    @Test
    public void testGetCheckpoint() {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.checkpoint = Instant.now().minus(1, ChronoUnit.DAYS);
        checkpoint.name = Checkpoint.Name.CREATES_WITHOUT_PANOPTA;

        when(monitorService.getCheckpoint(Checkpoint.Name.CREATES_WITHOUT_PANOPTA)).thenReturn(checkpoint);

        Checkpoint actualCheckpoint = vmActionsMonitorResource.getCheckpoint(Checkpoint.Name.CREATES_WITHOUT_PANOPTA);

        Assert.assertEquals(checkpoint.name, actualCheckpoint.name);
        Assert.assertEquals(checkpoint.checkpoint, actualCheckpoint.checkpoint);
    }

    @Test
    public void testDeleteCheckpoint() {
        doNothing().when(monitorService).deleteCheckpoint(Checkpoint.Name.CREATES_WITHOUT_PANOPTA);

        vmActionsMonitorResource.deleteCheckpoint(Checkpoint.Name.CREATES_WITHOUT_PANOPTA);

        verify(monitorService, times(1))
                .deleteCheckpoint(Checkpoint.Name.CREATES_WITHOUT_PANOPTA);
    }

    @Test
    public void testGetHvsBlockingSnapshots() {
        List<HvBlockingSnapshotsData> expectedHvBlockingSnapshotsData = new ArrayList<>();
        expectedHvBlockingSnapshotsData.add(new HvBlockingSnapshotsData("hypervisor1", UUID.randomUUID(),
                                                                        Instant.now().minus(Duration.ofDays(2))));
        when(monitorService.getHvsBlockingSnapshots(anyLong())).thenReturn(expectedHvBlockingSnapshotsData);
        List<HvBlockingSnapshotsData>
                actualHvBlockingSnapshotsData = vmActionsMonitorResource.getHvsBlockingSnapshots(anyLong());
        Assert.assertNotNull(actualHvBlockingSnapshotsData);
        Assert.assertEquals(expectedHvBlockingSnapshotsData.size(), actualHvBlockingSnapshotsData.size());
    }

    @Test
    public void testGetCreatesWithoutPanopta() {
        Action action = mock(Action.class);
        action.type = ActionType.CREATE_VM;
        List<Action> actions = new ArrayList<>();
        actions.add(action);
        ResultSubset<Action> result = new ResultSubset<>(actions, actions.size());
        when(vmActionService.getActionList(any())).thenReturn(result);
        when(vmActionService.getCreatesWithoutPanopta(anyLong())).thenReturn(actions);
        ActionTypeErrorData actualCreates = vmActionsMonitorResource.getCreatesWithoutPanopta(anyLong());
        Assert.assertEquals(1, actualCreates.affectedAccounts);
        Assert.assertEquals(100, actualCreates.failurePercentage, 0);
        Assert.assertEquals(ActionType.CREATE_VM, actualCreates.actionType);
        Assert.assertEquals(1, actualCreates.failedActions.size());
    }
}
