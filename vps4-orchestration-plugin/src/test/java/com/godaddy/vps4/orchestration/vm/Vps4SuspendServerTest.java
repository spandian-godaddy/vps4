package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4SuspendServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    Config config = mock(Config.class);
    VirtualMachine vm;

    Vps4SuspendServer command = new Vps4SuspendServer(actionService, creditService, config);

    @Before
    public void setup() {
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    @Test
    public void testAbuseSuspend() {
        Vps4SuspendServer.Request request = new Vps4SuspendServer.Request();
        request.virtualMachine = vm;
        request.actionType = ActionType.ABUSE_SUSPEND;

        command.executeWithAction(context, request);
        verify(creditService, times(1)).setStatus(vm.orionGuid, AccountStatus.ABUSE_SUSPENDED);
        verify(creditService, times(1))
                .setAbuseSuspendedFlag(vm.orionGuid, true);
        verify(context, times(1)).execute(StopVm.class, vm.hfsVmId);
    }

    @Test
    public void testBillingSuspend() {
        Vps4SuspendServer.Request request = new Vps4SuspendServer.Request();
        request.virtualMachine = vm;
        request.actionType = ActionType.BILLING_SUSPEND;

        command.executeWithAction(context, request);
        verify(creditService, times(1)).setStatus(vm.orionGuid, AccountStatus.SUSPENDED);
        verify(creditService, times(1))
                .setBillingSuspendedFlag(vm.orionGuid, true);
        verify(context, times(1)).execute(StopVm.class, vm.hfsVmId);
    }

    @Test
    public void testNoPausePanoptaMonitoringWhenConfigIsOff() {
        Vps4SuspendServer.Request request = new Vps4SuspendServer.Request();
        request.virtualMachine = vm;
        when(config.get("panopta.installation.enabled", "false")).thenReturn("false");
        command.executeWithAction(context, request);
        verify(context, times(0)).execute(eq(PausePanoptaMonitoring.class), any());
    }

    @Test
    public void testPausePanoptaMonitoring(){
        Vps4SuspendServer.Request request = new Vps4SuspendServer.Request();
        request.virtualMachine = vm;
        when(config.get("panopta.installation.enabled", "false")).thenReturn("true");
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(PausePanoptaMonitoring.class), any());
    }
}
