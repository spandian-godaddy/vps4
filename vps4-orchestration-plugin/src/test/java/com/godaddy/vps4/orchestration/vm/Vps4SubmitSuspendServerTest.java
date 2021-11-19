package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class Vps4SubmitSuspendServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;

    Vps4SubmitSuspendServer command = new Vps4SubmitSuspendServer(actionService, creditService);

    @Before
    public void setup() {
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    @Test
    public void testSubmitSuspend() throws Exception {
        Vps4SubmitSuspendServer.Request request = new Vps4SubmitSuspendServer.Request();
        request.virtualMachine = vm;
        request.reason = ECommCreditService.SuspensionReason.FRAUD;

        command.executeWithAction(context, request);
        verify(creditService, times(1)).submitSuspend(vm.orionGuid,
                ECommCreditService.SuspensionReason.FRAUD);
    }
}
