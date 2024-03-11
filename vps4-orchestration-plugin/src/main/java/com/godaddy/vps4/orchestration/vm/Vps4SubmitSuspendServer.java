package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;


@CommandMetadata(
        name = "Vps4SubmitSuspendServer",
        requestType = Vps4SubmitSuspendServer.Request.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SubmitSuspendServer extends ActionCommand<Vps4SubmitSuspendServer.Request, Void> {

    final ActionService actionService;
    final CreditService creditService;
    final ShopperNotesService shopperNotesService;
    private final Logger logger = LoggerFactory.getLogger(Vps4SubmitSuspendServer.class);

    @Inject
    public Vps4SubmitSuspendServer(ActionService actionService, CreditService creditService, ShopperNotesService shopperNotesService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
        this.shopperNotesService = shopperNotesService;
    }

    private void writeShopperNote(Request request) {
        try {
            String shopperNote = String.format("Server was suspended by %s with reason %s. VM ID: %s. Credit ID: %s.",
                    request.gdUsername, request.reason, request.virtualMachine.vmId,
                    request.virtualMachine.orionGuid);
            shopperNotesService.processShopperMessage(request.virtualMachine.vmId, shopperNote);
        } catch (Exception ignored) {}
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4SubmitSuspendServer.Request request) throws Exception {
        logger.info("Request: {}", request);
        creditService.submitSuspend(request.virtualMachine.orionGuid, request.reason);
        writeShopperNote(request);
        return null;
    }

    public static class Request extends VmActionRequest {
        public ECommCreditService.SuspensionReason reason;
        public String gdUsername;
    }

}
