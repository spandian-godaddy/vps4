package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.RescueVm;
import com.godaddy.vps4.orchestration.hfs.vm.StopVm;
import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@CommandMetadata(
        name = "Vps4ProcessSuspendServer",
        requestType = VmActionRequest.class,
        responseType = Void.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ProcessSuspendServer extends ActionCommand<VmActionRequest, Void> {

    final ActionService actionService;
    final CreditService creditService;
    private final Logger logger = LoggerFactory.getLogger(Vps4ProcessSuspendServer.class);

    @Inject
    public Vps4ProcessSuspendServer(ActionService actionService, CreditService creditService) {
        super(actionService);
        this.actionService = actionService;
        this.creditService = creditService;
    }

    @Override
    protected Void executeWithAction(CommandContext context, VmActionRequest request) {
        logger.info("Processing Suspend Service with Request: {}", request);

        pausePanoptaMonitoring(context, request);
        if(request.virtualMachine.spec.isVirtualMachine())
            suspendVm(context, request);
        else
            suspendDed(context, request);
        return null;
    }

    private void suspendDed(CommandContext context, VmActionRequest request) {
        context.execute(RescueVm.class, request.virtualMachine.hfsVmId);
    }

    public void pausePanoptaMonitoring(CommandContext context, VmActionRequest request) {
        context.execute(PausePanoptaMonitoring.class, request.virtualMachine.vmId);
    }

    protected void suspendVm(CommandContext context, VmActionRequest request) {
        context.execute(StopVm.class, request.virtualMachine.hfsVmId);
    }
}
