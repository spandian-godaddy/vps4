package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import gdg.hfs.orchestration.CommandContext;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.hfs.vm.VmAction.Status;
import com.godaddy.hfs.vm.VmService;

public class WaitForManageVmActionTest {

    VmService vmService = mock(VmService.class);
    CommandContext context = mock(CommandContext.class);

    WaitForManageVmAction command = new WaitForManageVmAction(vmService);
    long hfsVmId = 42;
    long hfsVmActionId = 23;
    long magicSleepTime = 2000;

    private VmAction createMockVmAction(Status status) {
        VmAction mockAction = mock(VmAction.class);
        mockAction.vmId = hfsVmId;
        mockAction.vmActionId = hfsVmActionId;
        mockAction.state = status;
        return mockAction;
    }

    @Test
    public void testExecuteWaitForComplete() {
        VmAction newHfsAction = createMockVmAction(Status.NEW);
        VmAction requestedHfsAction = createMockVmAction(Status.REQUESTED);
        VmAction inProgressHfsAction = createMockVmAction(Status.IN_PROGRESS);
        VmAction completeHfsAction = createMockVmAction(Status.COMPLETE);

        // simulates vm action progressing new->requested->in_progress->complete
        when(vmService.getVmAction(hfsVmId, hfsVmActionId))
            .thenReturn(requestedHfsAction)
            .thenReturn(inProgressHfsAction)
            .thenReturn(completeHfsAction);

        VmAction result = command.execute(context, newHfsAction);
        verify(vmService, times(3)).getVmAction(hfsVmId, hfsVmActionId);
        verify(context, times(3)).sleep(magicSleepTime);
        assertEquals(completeHfsAction, result);
    }

    @Test(expected=RuntimeException.class)
    public void testExecuteActionFails() {
        VmAction newHfsAction = createMockVmAction(Status.NEW);
        VmAction errorHfsAction = createMockVmAction(Status.ERROR);

        when(vmService.getVmAction(hfsVmId, hfsVmActionId)).thenReturn(errorHfsAction);

        command.execute(context, newHfsAction);
    }

}
