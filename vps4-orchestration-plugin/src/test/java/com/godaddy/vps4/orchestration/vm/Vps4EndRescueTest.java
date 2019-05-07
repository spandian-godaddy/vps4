package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4EndRescueTest {

    private ActionService actionService = mock(ActionService.class);
    private VmService vmService = mock(VmService.class);

    private Vps4EndRescue command = new Vps4EndRescue(actionService, vmService);
    private CommandContext context = mock(CommandContext.class);
    private long hfsVmId = 42L;

    @Test
    public void testExecuteWithActionSuccess() {
        VmActionRequest request = new VmActionRequest();
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = hfsVmId;
        request.virtualMachine = vm;

        assertNull(command.executeWithAction(context, request));
        verify(context).execute(EndRescueVm.class, hfsVmId);
    }

}
