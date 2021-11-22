package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.hfs.vm.EndRescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4ProcessReinstateMessageTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;

    Vps4ProcessReinstateServer command = new Vps4ProcessReinstateServer(actionService, creditService);

    @Before
    public void setup() {
        vm = mock(VirtualMachine.class);
        vm.spec = mock(ServerSpec.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    @Test
    public void testReinstateVirtual() {
        when(vm.spec.isVirtualMachine()).thenReturn(true);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(context, times(1)).execute(StartVm.class, vm.hfsVmId);
    }

    @Test
    public void testReinstateDed() {
        when(vm.spec.isVirtualMachine()).thenReturn(false);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(context, times(1)).execute(EndRescueVm.class, vm.hfsVmId);
    }

    @Test
    public void testResumePanoptaMonitoring(){
        Vps4ReinstateServer.Request request = new Vps4ReinstateServer.Request();
        request.virtualMachine = vm;
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(ResumePanoptaMonitoring.class), any());
    }
}
