package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4ReinstateDedicatedTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;

    Vps4ReinstateDedicated command = new Vps4ReinstateDedicated(actionService, creditService);

    @Before
    public void setup () {
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    @Test
    public void testReinstateDed() throws Exception {
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(creditService).setStatus(vm.orionGuid, AccountStatus.ACTIVE);
        verify(context).execute(EndRescueVm.class, vm.hfsVmId);
    }

}
