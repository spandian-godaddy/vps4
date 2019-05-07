package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4RescueTest {

    private ActionService actionService = mock(ActionService.class);

    private Vps4Rescue command = new Vps4Rescue(actionService);
    private CommandContext context = mock(CommandContext.class);
    private long hfsVmId = 42L;

    @Test
    public void testExecuteWithActionSuccess() throws Exception {
        VmActionRequest request = new VmActionRequest();
        VirtualMachine vm = mock(VirtualMachine.class);
        VmAction hfsAction = new VmAction();
        hfsAction.vmActionId = 123L;
        when(context.execute(RescueVm.class, hfsVmId)).thenReturn(hfsAction);
        vm.hfsVmId = hfsVmId;
        request.virtualMachine = vm;

        Vps4Rescue.Response response = command.executeWithAction(context, request);
        assertEquals(hfsAction.vmActionId, response.hfsVmActionId);
        verify(context).execute(RescueVm.class, hfsVmId);
    }
}
