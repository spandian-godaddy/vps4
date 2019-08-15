package com.godaddy.vps4.orchestration.vm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.StartVm;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.ActionService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4ReinstateServer",
        requestType = Vps4ReinstateServer.Request.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ReinstateServer extends ActionCommand<Vps4ReinstateServer.Request, Void> {

    final ActionService actionService;
    final CreditService creditService;
    private final Config config;
    private final Logger logger = LoggerFactory.getLogger(Vps4ReinstateServer.class);

    @Inject
    public Vps4ReinstateServer(ActionService actionService, CreditService creditService, Config config) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
        this.config = config;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4ReinstateServer.Request request) {
        logger.info("Request: {}", request);
        reinstateVm(context, request);
        resumePanoptaMonitoring(context, request);
        updateCredit(request);
        return null;
    }

    private void updateCredit(Vps4ReinstateServer.Request request) {
        creditService.setStatus(request.virtualMachine.orionGuid, AccountStatus.ACTIVE);
        creditService.updateProductMeta(request.virtualMachine.orionGuid, request.resetFlag, String.valueOf(false));
    }

    public void resumePanoptaMonitoring(CommandContext context,  Vps4ReinstateServer.Request request) {
        boolean isPanoptaInstallationEnabled = Boolean.valueOf(config.get("panopta.installation.enabled", "false"));
        if (isPanoptaInstallationEnabled) {
            context.execute(ResumePanoptaMonitoring.class, request.virtualMachine.vmId);
        }
    }

    protected void reinstateVm(CommandContext context, Vps4ReinstateServer.Request request) {
        context.execute(StartVm.class, request.virtualMachine.hfsVmId);
    }

    public static class Request extends VmActionRequest {
        public ECommCreditService.ProductMetaField resetFlag;
    }
}
