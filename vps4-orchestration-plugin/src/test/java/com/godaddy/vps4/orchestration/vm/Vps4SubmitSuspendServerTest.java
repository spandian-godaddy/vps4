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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class Vps4SubmitSuspendServerTest {
    ActionService actionService = mock(ActionService.class);
    CreditService creditService = mock(CreditService.class);
    ShopperNotesService shopperNotesService = mock(ShopperNotesService.class);
    CommandContext context = mock(CommandContext.class);
    VirtualMachine vm;

    Vps4SubmitSuspendServer command = new Vps4SubmitSuspendServer(actionService, creditService, shopperNotesService);

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

    @Test
    public void testSubmitSuspendCreatesShopperNotes() throws Exception {
        Vps4SubmitSuspendServer.Request request = new Vps4SubmitSuspendServer.Request();
        request.virtualMachine = vm;
        request.reason = ECommCreditService.SuspensionReason.FRAUD;
        request.gdUsername = "gdUser";

        String shopperNote = String.format("Server was suspended by %s with reason %s. VM ID: %s. Credit ID: %s.",
                request.gdUsername, request.reason, request.virtualMachine.vmId,
                request.virtualMachine.orionGuid);

        command.executeWithAction(context, request);
        verify(shopperNotesService, times(1)).processShopperMessage(vm.vmId,
                shopperNote);
    }
}
