package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordService;

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

}
