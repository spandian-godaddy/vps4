package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
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
    private final Logger logger = LoggerFactory.getLogger(Vps4SubmitReinstateServer.class);

    @Inject
    public Vps4SubmitReinstateServer(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4SubmitReinstateServer.Request request) throws Exception {
        logger.info("Request: {}", request);
        creditService.submitReinstate(request.virtualMachine.orionGuid, request.reason);
        return null;
    }

    public static class Request extends VmActionRequest {
        public ECommCreditService.SuspensionReason reason;
    }
}
