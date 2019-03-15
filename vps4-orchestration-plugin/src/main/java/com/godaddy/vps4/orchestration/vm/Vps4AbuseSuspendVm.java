package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;


@CommandMetadata(
        name="Vps4AbuseSuspendVm",
        requestType=VmActionRequest.class,
        responseType=Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4AbuseSuspendVm extends ActionCommand<VmActionRequest, Void> {

    private final Logger logger = LoggerFactory.getLogger(Vps4AbuseSuspendVm.class);

    final ActionService actionService;
    final CreditService creditService;

    @Inject
    public Vps4AbuseSuspendVm(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) throws Exception {
        logger.info("Request: {}", request);
        setAccountStatusToAbuseSuspend(request);
        suspendVm(context, request);
        return null;
    }

    private void setAccountStatusToAbuseSuspend(VmActionRequest request) {
        creditService.setStatus(request.virtualMachine.orionGuid, AccountStatus.ABUSE_SUSPENDED);
    }

    protected void suspendVm(CommandContext context, VmActionRequest request) {
        context.execute(StopVm.class, request.virtualMachine.hfsVmId);
    }

}
