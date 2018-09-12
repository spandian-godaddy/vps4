package com.godaddy.vps4.web.action;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Exception;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ActionResourceTest {


    private ActionService actionService = mock(ActionService.class);
    private ActionResource actionResource;

    @Before
    public void setup() {
        actionResource = new ActionResource(actionService);
    }

    @Test
    public void testGetVmActionsEmpty() {
        ResultSubset<Action> emptyResultSet = new ResultSubset<>(new ArrayList<>(), 0);
        when(actionService.getActions(null, 100, 0, new ArrayList<>(), null, null, null)).thenReturn(emptyResultSet);
        List<Action> actions = actionResource.getVmActions(null, null, 100, 0, null, null, null);
        Assert.assertEquals(0, actions.size());
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
    public void testGetVmActionsNullParams() {
        ResultSubset<Action> resultSubset = getTestResultSet(20, ActionType.CREATE_VM, ActionStatus.ERROR);
        when(actionService.getActions(null, 100, 0, new ArrayList<>(), null, null, null)).thenReturn(resultSubset);
        List<Action> actions = actionResource.getVmActions(null, null, 100, 0, null, null, null);
        Assert.assertEquals(resultSubset.totalRows, actions.size());
    }

    @Test
    public void testGetVmActions() {
        ResultSubset<Action> resultSubset = getTestResultSet(20, ActionType.CREATE_VM, ActionStatus.ERROR);

        UUID testVmId = UUID.randomUUID();
        ActionStatus testStatus = ActionStatus.ERROR;
        Instant testBeginDate = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant testEndDate = Instant.now();
        ActionType testActionType = ActionType.CREATE_VM;

        List<String> listOfStatus = new ArrayList<>();
        listOfStatus.add(testStatus.toString());

        when(actionService.getActions(testVmId, 100, 0, listOfStatus, testBeginDate, testEndDate, testActionType)).thenReturn(resultSubset);
        List<Action> actions = actionResource.getVmActions(testVmId, testActionType, 100, 0, testStatus, testBeginDate.toString(), testEndDate.toString());
        Assert.assertEquals(resultSubset.totalRows, actions.size());
    }

    @Test(expected = Vps4Exception.class)
    public void testGetVmActionsInvalidDate() {
        actionResource.getVmActions(null, null, 100, 0, null, "hello", null);
    }
}
