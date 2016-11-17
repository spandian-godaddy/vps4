package com.godaddy.vps4.vm;


import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.web.vm.ManageVmAction;
import com.godaddy.vps4.web.vm.ManageVmWorker;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Created by abhoite on 11/10/16.
 */
public class ManageVmWorkerTest {
    long vmId = 1;
    VmService vmService;
    ActionService actionService;
    Vps4User user;
    VmAction inProgressAction;
    VmAction completeAction;
    VmAction failedAction;
    ManageVmAction action;
    ManageVmWorker worker;

    private VmAction buildAction(VmAction.Status status) {
        VmAction action = new VmAction();
        action.state = status;
        return action;
    }

    @Before
    public void setupTest() {
        inProgressAction = buildAction(VmAction.Status.IN_PROGRESS);
        completeAction = buildAction(VmAction.Status.COMPLETE);
        failedAction = buildAction(VmAction.Status.FAILED);
        user = new Vps4User(0, "fakeShopper");

        vmService = mock(VmService.class);
        actionService = mock(ActionService.class);
    }

    @Test
    public void testStartVmRun() {
        action = new ManageVmAction(vmId, ActionType.START_VM);
        worker = new ManageVmWorker(vmService, actionService, vmId, action);
        when(actionService.createAction(vmId, action.getActionType(), "{}", user.getId())).thenReturn((long) 0);
        when(vmService.startVm(vmId)).thenReturn(inProgressAction);
        when(vmService.getVmAction(vmId, inProgressAction.vmActionId)).thenReturn(completeAction);
        worker.run();
        assertNotNull("Action object cannot be null.", action);
        assertEquals("Expected action status value does not match actual value.", action.status, ActionStatus.COMPLETE);
        verify(actionService, atLeastOnce()).completeAction(eq(action.getActionId()), anyString(), anyString());
    }

    @Test
    public void testStopVmRun() {
        action = new ManageVmAction(vmId, ActionType.STOP_VM);
        worker = new ManageVmWorker(vmService, actionService, vmId, action);
        when(actionService.createAction(vmId, action.getActionType(), "{}", user.getId())).thenReturn((long) 0);
        when(vmService.stopVm(vmId)).thenReturn(inProgressAction);
        when(vmService.getVmAction(vmId, inProgressAction.vmActionId)).thenReturn(completeAction);
        worker.run();
        assertNotNull("Action object cannot be null.", action);
        assertEquals("Expected action status value does not match actual value", action.status, ActionStatus.COMPLETE);
        verify(actionService, atLeastOnce()).completeAction(eq(action.getActionId()), anyString(), anyString());
    }

    @Test(expected = Vps4Exception.class)
    public void testStartVmRunUnsupportedException() {
        action = new ManageVmAction(vmId, ActionType.START_VM);
        worker = new ManageVmWorker(vmService, actionService, vmId, action);
        when(actionService.createAction(vmId, action.getActionType(), "{}", user.getId())).thenReturn((long) 0);
        when(vmService.startVm(vmId)).thenReturn(null);
        worker.run();
        assertEquals("Expected action status value does not match actual value.", action.status, ActionStatus.ERROR);
        verify(actionService, atLeastOnce()).failAction(eq(action.getActionId()), anyString(), anyString());
    }

    @Test
    public void testStartVmRunFailedAction() {
        action = new ManageVmAction(vmId, ActionType.START_VM);
        worker = new ManageVmWorker(vmService, actionService, vmId, action);
        when(actionService.createAction(vmId, action.getActionType(), "{}", user.getId())).thenReturn((long) 0);
        when(vmService.startVm(vmId)).thenReturn(inProgressAction);
        when(vmService.getVmAction(vmId, inProgressAction.vmActionId)).thenReturn(failedAction);
        worker.run();
        assertEquals("Expected action status value does not match actual value.", action.status, ActionStatus.ERROR);
        assertTrue("Message should indicate that it was unable to complete action. ", action.getMessage().contains("Could not complete action"));
        verify(actionService, atLeastOnce()).failAction(eq(action.getActionId()), anyString(), anyString());
    }

    @Test
    public void testStartVmRunTimeout() {
        action = new ManageVmAction(vmId, ActionType.START_VM);
        worker = new ManageVmWorker(vmService, actionService, vmId, action, 4000);
        when(actionService.createAction(vmId, action.getActionType(), "{}", user.getId())).thenReturn((long) 0);
        when(vmService.startVm(vmId)).thenReturn(inProgressAction);
        when(vmService.getVmAction(vmId, inProgressAction.vmActionId)).thenReturn(inProgressAction);
        worker.run();
        assertEquals("Expected action status value does not match actual value.", action.status, ActionStatus.ERROR);
        assertTrue("Message should indicate that timeout occurred. ", action.getMessage().contains("Timeout"));
        verify(actionService, atLeastOnce()).failAction(eq(action.getActionId()), anyString(), anyString());
    }

    @Test
    public void testRestartVm() {
        action = new ManageVmAction(vmId, ActionType.STOP_VM);
        worker = new ManageVmWorker(vmService, actionService, vmId, action, 4000);
        when(actionService.createAction(vmId, action.getActionType(), "{}", user.getId())).thenReturn((long) 0);
        when(vmService.stopVm(vmId)).thenReturn(inProgressAction);
        when(vmService.getVmAction(vmId, inProgressAction.vmActionId)).thenReturn(completeAction);
        worker.run();
        verify(vmService, atLeastOnce()).stopVm(vmId);

        action = new ManageVmAction(vmId, ActionType.START_VM);
        worker = new ManageVmWorker(vmService, actionService, vmId, action, 4000);
        when(actionService.createAction(vmId, action.getActionType(), "{}", user.getId())).thenReturn((long) 0);
        when(vmService.startVm(vmId)).thenReturn(inProgressAction);
        when(vmService.getVmAction(vmId, inProgressAction.vmActionId)).thenReturn(completeAction);
        worker.run();
        verify(vmService, atLeastOnce()).startVm(vmId);

        verify(vmService, times(2)).getVmAction(vmId, completeAction.vmActionId);
        verify(actionService, times(2)).completeAction(eq(action.getActionId()), anyString(), anyString());
        assertEquals("Expected action status value does not match actual value.", action.status, ActionStatus.COMPLETE);
    }

}
