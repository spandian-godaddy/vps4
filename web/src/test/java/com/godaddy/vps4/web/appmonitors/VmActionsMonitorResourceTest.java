package com.godaddy.vps4.web.appmonitors;

import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
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
        when(monitorService.getVmsByActions(15L, ActionType.RESTART_VM, ActionStatus.IN_PROGRESS)).thenReturn(expectedVmActionData);
        List<VmActionData> actualVmActionData = vmActionsMonitorResource.getRestartPendingVms(15L);
        Assert.assertNotNull(actualVmActionData);
        Assert.assertEquals(expectedVmActionData.size(), actualVmActionData.size());
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
        for(long i = 0; i < size; i++) {
            Action action = new Action(i, UUID.randomUUID(), actionType, null, null, null,
                    actionStatus, Instant.now().minus(10, ChronoUnit.MINUTES), Instant.now(), null,
                    UUID.randomUUID(), "tester");
            testActions.add(action);
        }
        return new ResultSubset<>(testActions, testActions.size());
    }

    @Test
    public void testCheckForFailingActions() {
        ResultSubset<Action> resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.COMPLETE);
        resultSubset = getTestResultSet(5, ActionType.START_VM, ActionStatus.ERROR, resultSubset.results);
        ResultSubset<Action> emptyResultSet = new ResultSubset<>(new ArrayList<>(), 0);

        when(actionService.getActions(any(UUID.class), anyLong(), anyLong(), any(), any(Instant.class), any(Instant.class), any(ActionType.class))).thenReturn(emptyResultSet);
        when(actionService.getActions(null, 10, 0, new ArrayList<>(), null, null, ActionType.START_VM)).thenReturn(resultSubset);

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

        when(actionService.getActions(any(UUID.class), anyLong(), anyLong(), any(), any(Instant.class), any(Instant.class), any(ActionType.class))).thenReturn(emptyResultSet);
        when(actionService.getActions(null, 10, 0, new ArrayList<>(), null, null, ActionType.START_VM)).thenReturn(resultSubset);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10);
        Assert.assertEquals(1, errorData.size());
        ActionTypeErrorData actionTypeErrorData = errorData.get(0);
        Assert.assertEquals(3, actionTypeErrorData.failedActions.size());
        Assert.assertEquals(ActionType.START_VM, actionTypeErrorData.actionType);
        Assert.assertTrue("Expected 60%, actual " + actionTypeErrorData.failurePercentage, actionTypeErrorData.failurePercentage == 60.0);
    }

    @Test
    public void testCheckForFailingActionsEmpty() {
        ResultSubset<Action> emptyResultSet = new ResultSubset<>(new ArrayList<>(), 0);

        when(actionService.getActions(any(UUID.class), anyLong(), anyLong(), any(), any(Instant.class), any(Instant.class), any(ActionType.class))).thenReturn(emptyResultSet);

        List<ActionTypeErrorData> errorData = vmActionsMonitorResource.getFailedActionsForAllTypes(10);
        Assert.assertEquals(0, errorData.size());
    }
}
