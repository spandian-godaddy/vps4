package com.godaddy.vps4.sysadmin;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.godaddy.vps4.vm.UserService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.web.sysadmin.SetAdminAction;
import com.godaddy.vps4.web.sysadmin.SysAdminResource;
import com.godaddy.vps4.web.sysadmin.SysAdminResource.SetAdminRequest;

import gdg.hfs.vhfs.sysadmin.SysAdminAction;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class ToggleAdminWorkerTest {

    SysAdminService sysAdminService;
    UserService userService;
    SysAdminAction inProgressAction;
    SysAdminAction completeAction;
    long vmId = 1;
    String username = "testUsername";
    SysAdminResource resource;
    SetAdminRequest request;

    private SysAdminAction buildAction(SysAdminAction.Status status) {
        SysAdminAction action = new SysAdminAction();
        action.status = status;
        action.sysAdminActionId = 1234;
        return action;
    }

    @Before
    public void setupTest() {
        inProgressAction = buildAction(SysAdminAction.Status.IN_PROGRESS);
        completeAction = buildAction(SysAdminAction.Status.COMPLETE);

        sysAdminService = Mockito.mock(SysAdminService.class);
        when(sysAdminService.enableAdmin(vmId, username)).thenReturn(inProgressAction);
        when(sysAdminService.disableAdmin(vmId, username)).thenReturn(inProgressAction);
        when(sysAdminService.getSysAdminAction(inProgressAction.sysAdminActionId)).thenReturn(completeAction);

        userService = Mockito.mock(UserService.class);
        when(userService.userExists(username, vmId)).thenReturn(true);
        
        resource = new SysAdminResource(sysAdminService, userService);
        request = new SetAdminRequest();
        request.username = username;
    }

    private void waitForActionCompletion(SetAdminAction action) {
        Assert.assertEquals(ActionStatus.IN_PROGRESS, action.status);
        while (action.status.equals(ActionStatus.IN_PROGRESS)) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                System.out.println("Interrupted while sleeping");
            }
        }
    }

    @Test
    public void testEnableAdminAccess() {
        SetAdminAction returnAction = resource.enableUserAdmin(vmId, request);
        waitForActionCompletion(returnAction);
        verify(sysAdminService, times(1)).enableAdmin(vmId, username);
        verify(sysAdminService, times(0)).disableAdmin(vmId, username);
        Assert.assertEquals(ActionStatus.COMPLETE, returnAction.status);
    }

    @Test
    public void testDisableAdminAccess() {
        SetAdminAction returnAction = resource.disableUserAdmin(vmId, request);
        waitForActionCompletion(returnAction);
        verify(sysAdminService, times(1)).enableAdmin(vmId, username);
        verify(sysAdminService, times(1)).disableAdmin(vmId, username);
        Assert.assertEquals(ActionStatus.COMPLETE, returnAction.status);
    }

    @Test
    public void testFailEnableCase() {
        completeAction.status = SysAdminAction.Status.FAILED;
        SetAdminAction returnAction = resource.enableUserAdmin(vmId, request);
        waitForActionCompletion(returnAction);
        verify(sysAdminService, times(1)).enableAdmin(vmId, username);
        verify(sysAdminService, times(0)).disableAdmin(vmId, username);
        Assert.assertEquals(ActionStatus.ERROR, returnAction.status);
    }
    
    @Test
    public void testUserDoesntExist(){
        when(userService.userExists(username, vmId)).thenReturn(false); // overwrites the one defined in setup
        
        SetAdminAction returnAction = resource.disableUserAdmin(vmId, request);
        verify(sysAdminService, times(0)).enableAdmin(Mockito.anyLong(), Mockito.anyString());
        verify(sysAdminService, times(0)).disableAdmin(Mockito.anyLong(), Mockito.anyString());
        Assert.assertEquals(ActionStatus.INVALID, returnAction.status);
    }

    @Test
    public void testFailDisableCase() {
        Answer<SysAdminAction> answer = new Answer<SysAdminAction>() {
            // returns 1 complete, then 1 failed, then repeats
            private int timesCalled = 0;

            public SysAdminAction answer(InvocationOnMock invocation) throws Throwable {
                if (timesCalled < 1) {
                    timesCalled++;
                    return buildAction(SysAdminAction.Status.COMPLETE);
                }
                timesCalled = 0;
                return buildAction(SysAdminAction.Status.FAILED);
            }
        };
        completeAction.status = SysAdminAction.Status.FAILED;
        when(sysAdminService.getSysAdminAction(inProgressAction.sysAdminActionId)).thenAnswer(answer);
        SetAdminAction returnAction = resource.disableUserAdmin(vmId, request);
        waitForActionCompletion(returnAction);
        verify(sysAdminService, times(1)).enableAdmin(vmId, username);
        verify(sysAdminService, times(1)).disableAdmin(vmId, username);
        Assert.assertEquals(ActionStatus.ERROR, returnAction.status);
    }
}
