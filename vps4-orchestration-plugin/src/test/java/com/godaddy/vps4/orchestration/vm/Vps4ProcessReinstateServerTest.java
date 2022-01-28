package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4ProcessReinstateServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;
    VirtualMachineCredit credit;

    Vps4ProcessReinstateServer command = new Vps4ProcessReinstateServer(actionService, creditService);

    @Before
    public void setup() {
        vm = mock(VirtualMachine.class);
        vm.spec = mock(ServerSpec.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
        credit = mock(VirtualMachineCredit.class);
        when(creditService.getVirtualMachineCredit(vm.orionGuid)).thenReturn(credit);
    }

    @Test
    public void testReinstateVirtual() {
        when(vm.spec.isVirtualMachine()).thenReturn(true);
        when(credit.isAccountSuspended()).thenReturn(false);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(context, times(1)).execute(StartVm.class, vm.hfsVmId);
        verify(creditService, times(1)).updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.SUSPENDED,
                null);
    }

    @Test
    public void testReinstateDed() {
        when(vm.spec.isVirtualMachine()).thenReturn(false);
        when(credit.isAccountSuspended()).thenReturn(false);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        command.executeWithAction(context, request);
        verify(context, times(1)).execute(EndRescueVm.class, vm.hfsVmId);
        verify(creditService, times(1)).updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.SUSPENDED,
                null);
    }

    @Test
    public void testResumePanoptaMonitoring(){
        VmActionRequest request = new VmActionRequest();
        when(credit.isAccountSuspended()).thenReturn(false);
        request.virtualMachine = vm;
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(ResumePanoptaMonitoring.class), any());
    }

    @Test
    public void testReinstateFailsForSuspendedAccount() {
        when(credit.isAccountSuspended()).thenReturn(true);
        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;

        try {
            command.executeWithAction(context, request);
        }
        catch (RuntimeException e) {
            verify(context, never()).execute(StartVm.class, vm.hfsVmId);
        }
    }
}
