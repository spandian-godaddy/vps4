package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.shopperNotes.ShopperNotesService;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


@CommandMetadata(
        name = "Vps4SubmitReinstateServer",
        requestType = Vps4SubmitReinstateServer.Request.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SubmitReinstateServer extends ActionCommand<Vps4SubmitReinstateServer.Request, Void> {

    final ActionService actionService;
    final CreditService creditService;
    final ShopperNotesService shopperNotesService;
    private final Logger logger = LoggerFactory.getLogger(Vps4SubmitReinstateServer.class);

    @Inject
    public Vps4SubmitReinstateServer(ActionService actionService, CreditService creditService, ShopperNotesService shopperNotesService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
        this.shopperNotesService = shopperNotesService;
    }

    private void writeShopperNote(Vps4SubmitReinstateServer.Request request) {
        try {
            String shopperNote = String.format("Server was reinstated by %s with reason %s. VM ID: %s. Credit ID: %s.",
                    request.gdUsername, request.reason, request.virtualMachine.vmId,
                    request.virtualMachine.orionGuid);
            shopperNotesService.processShopperMessage(request.virtualMachine.vmId, shopperNote);
        } catch (Exception ignored) {}
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4SubmitReinstateServer.Request request) throws Exception {
        logger.info("Request: {}", request);
        if (request.reason == ECommCreditService.SuspensionReason.FRAUD ||
            request.reason == ECommCreditService.SuspensionReason.POLICY) {
            creditService.submitReinstate(request.virtualMachine.orionGuid, ECommCreditService.SuspensionReason.FRAUD);
            creditService.submitReinstate(request.virtualMachine.orionGuid, ECommCreditService.SuspensionReason.POLICY);
        } else  {
            creditService.submitReinstate(request.virtualMachine.orionGuid, request.reason);
        }
        writeShopperNote(request);
        return null;
    }

    public static class Request extends VmActionRequest {
        public ECommCreditService.SuspensionReason reason;
        public String gdUsername;
    }
}
