package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4AbuseSuspendDedicatedTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;

    Vps4AbuseSuspendDedicated command = new Vps4AbuseSuspendDedicated(actionService, creditService);

    @Before
    public void setup () {
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    @Test
    public void testAbuseSuspendDed() throws Exception {
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(creditService, times(1)).setStatus(vm.orionGuid, AccountStatus.ABUSE_SUSPENDED);
        verify(context, times(1)).execute(RescueVm.class, vm.hfsVmId);
    }

}