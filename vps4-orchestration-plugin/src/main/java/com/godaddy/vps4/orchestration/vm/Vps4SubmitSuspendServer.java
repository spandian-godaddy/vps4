package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

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
    private final Logger logger = LoggerFactory.getLogger(Vps4SubmitSuspendServer.class);

    @Inject
    public Vps4SubmitSuspendServer(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4SubmitSuspendServer.Request request) throws Exception {
        logger.info("Request: {}", request);
        creditService.submitSuspend(request.virtualMachine.orionGuid, request.reason);
        return null;
    }

    public static class Request extends VmActionRequest {
        public ECommCreditService.SuspensionReason reason;
    }

}
