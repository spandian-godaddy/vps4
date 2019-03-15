package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name="Vps4ReinstateVm",
        requestType=VmActionRequest.class,
        responseType=Void.class,
        retryStrategy=CommandRetryStrategy.NEVER
    )
public class Vps4ReinstateVm extends ActionCommand<VmActionRequest, Void> {

    private final Logger logger = LoggerFactory.getLogger(Vps4ReinstateVm.class);

    final ActionService actionService;
    final CreditService creditService;

    @Inject
    public Vps4ReinstateVm(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) throws Exception {
        logger.info("Request: {}", request);
        setAccountStatusToActive(request);
        reinstateVm(context, request);
        return null;
    }

    private void setAccountStatusToActive(VmActionRequest request) {
        creditService.setStatus(request.virtualMachine.orionGuid, AccountStatus.ACTIVE);
    }

    protected void reinstateVm(CommandContext context, VmActionRequest request) {
        context.execute(StartVm.class, request.virtualMachine.hfsVmId);
    }

}
