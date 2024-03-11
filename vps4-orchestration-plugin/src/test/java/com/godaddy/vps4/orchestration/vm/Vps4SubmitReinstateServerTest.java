package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class Vps4SubmitReinstateServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    ShopperNotesService shopperNotesService = mock(ShopperNotesService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;

    Vps4SubmitReinstateServer command = new Vps4SubmitReinstateServer(actionService, creditService, shopperNotesService);

    @Before
    public void setup() {
        vm = mock(VirtualMachine.class);
        vm.vmId = UUID.randomUUID();
        vm.orionGuid = UUID.randomUUID();
    }

    @Test
    public void testSubmitSuspend() throws Exception {
        Vps4SubmitReinstateServer.Request request = new Vps4SubmitReinstateServer.Request();
        request.virtualMachine = vm;
        request.reason = ECommCreditService.SuspensionReason.LEGAL;

        command.executeWithAction(context, request);
        verify(creditService, times(1)).submitReinstate(vm.orionGuid,
                ECommCreditService.SuspensionReason.LEGAL);
    }

    @Test
    public void testSubmitSuspendSendsShopperNote() throws Exception {
        Vps4SubmitReinstateServer.Request request = new Vps4SubmitReinstateServer.Request();
        request.virtualMachine = vm;
        request.reason = ECommCreditService.SuspensionReason.LEGAL;
        request.gdUsername = "gdUser";

        String shopperNote = String.format("Server was reinstated by %s with reason %s. VM ID: %s. Credit ID: %s.",
                request.gdUsername, request.reason, request.virtualMachine.vmId,
                request.virtualMachine.orionGuid);

        command.executeWithAction(context, request);
        verify(shopperNotesService, times(1)).processShopperMessage(vm.vmId,
                shopperNote);
    }

    @Test
    public void testSubmitSuspendForBothPolicyAndLegal() throws Exception {
        Vps4SubmitReinstateServer.Request request = new Vps4SubmitReinstateServer.Request();
        request.virtualMachine = vm;
        request.reason = ECommCreditService.SuspensionReason.FRAUD;

        command.executeWithAction(context, request);
        verify(creditService, times(1)).submitReinstate(vm.orionGuid,
                ECommCreditService.SuspensionReason.POLICY);
        verify(creditService, times(1)).submitReinstate(vm.orionGuid,
                ECommCreditService.SuspensionReason.FRAUD);
    }
}
