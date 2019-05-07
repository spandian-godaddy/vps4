package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;

public class Vps4ReinstateServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;

    Vps4ReinstateServer command = new Vps4ReinstateServer(actionService, creditService);

    @Before
    public void setup() {
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    @Test
    public void testReinstateAbuseSuspendedServer() {
        Vps4ReinstateServer.Request request = new Vps4ReinstateServer.Request();
        request.virtualMachine = vm;
        request.resetFlag = ECommCreditService.ProductMetaField.ABUSE_SUSPENDED_FLAG;

        command.executeWithAction(context, request);
        verify(creditService).setStatus(vm.orionGuid, AccountStatus.ACTIVE);
        verify(creditService).updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.ABUSE_SUSPENDED_FLAG,
                String.valueOf(false));
        verify(context).execute(StartVm.class, vm.hfsVmId);
    }

    @Test
    public void testReinstateBillingSuspendedServer() {
        Vps4ReinstateServer.Request request = new Vps4ReinstateServer.Request();
        request.virtualMachine = vm;
        request.resetFlag = ECommCreditService.ProductMetaField.BILLING_SUSPENDED_FLAG;

        command.executeWithAction(context, request);
        verify(creditService).setStatus(vm.orionGuid, AccountStatus.ACTIVE);
        verify(creditService).updateProductMeta(vm.orionGuid, ECommCreditService.ProductMetaField.BILLING_SUSPENDED_FLAG,
                                                String.valueOf(false));
        verify(context).execute(StartVm.class, vm.hfsVmId);
    }
}
