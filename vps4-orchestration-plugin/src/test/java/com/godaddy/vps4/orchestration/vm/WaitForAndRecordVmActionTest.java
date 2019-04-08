package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;

import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import org.junit.Test;

import gdg.hfs.orchestration.CommandContext;

public class WaitForAndRecordVmActionTest {

    HfsVmTrackingRecordService hfsVmTrackingRecordService = mock(HfsVmTrackingRecordService.class);
    CommandContext context = mock(CommandContext.class);
    WaitForAndRecordVmAction command = new WaitForAndRecordVmAction(hfsVmTrackingRecordService);
    
    private VmAction createMockVmAction(String actionType) {
        VmAction mockAction = mock(VmAction.class);
        mockAction.vmId = 42;
        mockAction.actionType = actionType;
        return mockAction;
    }

    @Test
    public void testExecuteWaitForAndRecordVmActionCreate() {
        VmAction newHfsAction = createMockVmAction("CREATE");
        when(context.execute(eq(WaitForVmAction.class), eq(newHfsAction))).thenReturn(newHfsAction);
        command.execute(context, newHfsAction);
        verify(hfsVmTrackingRecordService, times(1)).setHfsVmCreated(newHfsAction.vmId);
    }

    @Test
    public void testExecuteWaitForAndRecordVmActionDelete() {
        VmAction newHfsAction = createMockVmAction("DESTROY");
        when(context.execute(eq(WaitForVmAction.class), eq(newHfsAction))).thenReturn(newHfsAction);
        command.execute(context, newHfsAction);
        verify(hfsVmTrackingRecordService, times(1)).setHfsVmDestroyed(newHfsAction.vmId);
    }


}
