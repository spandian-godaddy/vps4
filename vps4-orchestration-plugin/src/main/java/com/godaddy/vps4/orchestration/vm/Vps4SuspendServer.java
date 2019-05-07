package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;


@CommandMetadata(
        name = "Vps4SuspendServer",
        requestType = Vps4SuspendServer.Request.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SuspendServer extends ActionCommand<Vps4SuspendServer.Request, Void> {

    final ActionService actionService;
    final CreditService creditService;
    private final Logger logger = LoggerFactory.getLogger(Vps4SuspendServer.class);

    @Inject
    public Vps4SuspendServer(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4SuspendServer.Request request) {
        logger.info("Request: {}", request);
        updateCredit(request);
        suspendVm(context, request);
        return null;
    }

    private void updateCredit(Vps4SuspendServer.Request request) {
        if(request.actionType == ActionType.ABUSE_SUSPEND) {

            creditService.setStatus(request.virtualMachine.orionGuid, AccountStatus.ABUSE_SUSPENDED);
            creditService.setAbuseSuspendedFlag(request.virtualMachine.orionGuid, true);

        } else if(request.actionType == ActionType.BILLING_SUSPEND) {

            creditService.setStatus(request.virtualMachine.orionGuid, AccountStatus.SUSPENDED);
            creditService.setBillingSuspendedFlag(request.virtualMachine.orionGuid, true);

        }
    }

    protected void suspendVm(CommandContext context, Vps4SuspendServer.Request request) {
        context.execute(StopVm.class, request.virtualMachine.hfsVmId);
    }

    public static class Request extends VmActionRequest {
        public ActionType actionType;
    }

}
