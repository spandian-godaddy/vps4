package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4SuspendDedServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    Config config = mock(Config.class);
    VirtualMachine vm;

    Vps4SuspendDedServer command = new Vps4SuspendDedServer(actionService, creditService, config);

    @Before
    public void setup () {
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    @Test
    public void testAbuseSuspendDed() {
        Vps4SuspendServer.Request request = new Vps4SuspendServer.Request();
        request.virtualMachine = vm;
        request.actionType = ActionType.ABUSE_SUSPEND;

        command.executeWithAction(context, request);
        verify(creditService, times(1)).setStatus(vm.orionGuid, AccountStatus.ABUSE_SUSPENDED);
        verify(creditService, times(1))
                .setAbuseSuspendedFlag(vm.orionGuid, true);
        verify(context, times(1)).execute(RescueVm.class, vm.hfsVmId);
    }

    @Test
    public void testBillingSuspendDed() {
        Vps4SuspendServer.Request request = new Vps4SuspendServer.Request();
        request.virtualMachine = vm;
        request.actionType = ActionType.BILLING_SUSPEND;

        command.executeWithAction(context, request);
        verify(creditService, times(1)).setStatus(vm.orionGuid, AccountStatus.SUSPENDED);
        verify(creditService, times(1))
                .setBillingSuspendedFlag(vm.orionGuid, true);
        verify(context, times(1)).execute(RescueVm.class, vm.hfsVmId);
    }
}