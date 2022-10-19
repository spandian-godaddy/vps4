package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmOutage;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CommandMetadata(
        name = "Vps4ClearVmOutage",
        requestType = Vps4ClearVmOutage.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ClearVmOutage extends ActionCommand<Vps4ClearVmOutage.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4ClearVmOutage.class);

    private final CreditService creditService;
    private final Config config;

    private CommandContext context;

    @Inject
    public Vps4ClearVmOutage(ActionService actionService, CreditService creditService, Config config) {
        super(actionService);
        this.creditService = creditService;
        this.config = config;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;
        if(request.virtualMachine.isCanceledOrDeleted())
        {
            logger.info("VM {} is not active, no need to clear outage {}", request.virtualMachine.vmId, request.outageId);
            return null;
        }
        VmOutage outage = getVmOutage(request);
        logger.info("Clearing outage {} for VM {}", request.outageId, request.virtualMachine.vmId);
        sendOutageNotificationEmail(request.virtualMachine, outage);
        executeUpdateJsdTicket(request.virtualMachine, outage);
        return null;
    }

    private VmOutage getVmOutage(Request request) {
        GetPanoptaOutage.Request getOutageRequest = new GetPanoptaOutage.Request();
        getOutageRequest.vmId = request.virtualMachine.vmId;
        getOutageRequest.outageId = request.outageId;
        VmOutage outage = context.execute("GetPanoptaOutage", GetPanoptaOutage.class, getOutageRequest);
        return outage;
    }

    private void executeUpdateJsdTicket(VirtualMachine virtualMachine, VmOutage outage) {
        boolean shouldUpdateJsdTicket = Boolean.parseBoolean(config.get("jsd.enabled", "false"));
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);

        if (credit != null && credit.isAccountActive() && virtualMachine.isActive()
                && credit.isManaged()
                && shouldUpdateJsdTicket) {
            updateJsdTicket(virtualMachine, outage);
        }
    }

    private void updateJsdTicket(VirtualMachine virtualMachine, VmOutage vmOutage) {
        ClearJsdOutageTicket.Request clearOutageJsdTicketRequest = new ClearJsdOutageTicket.Request();
        clearOutageJsdTicketRequest.vmId = virtualMachine.vmId;
        clearOutageJsdTicketRequest.outageMetrics = vmOutage.metricTypeMapper();
        clearOutageJsdTicketRequest.outageTimestamp = vmOutage.ended;
        clearOutageJsdTicketRequest.outageId = vmOutage.panoptaOutageId;
        context.execute("ClearJsdOutageTicket", ClearJsdOutageTicket.class, clearOutageJsdTicketRequest);
    }

    private void sendOutageNotificationEmail(VirtualMachine virtualMachine, VmOutage vmOutage) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);
        if (credit != null && credit.isAccountActive() && virtualMachine.isActive() && !credit.isManaged()) {
            VmOutageEmailRequest vmOutageEmailRequest =
                    new VmOutageEmailRequest(virtualMachine.name, virtualMachine.primaryIpAddress.ipAddress,
                            credit.getOrionGuid(), credit.getCustomerId(), virtualMachine.vmId, credit.isManaged(),
                            vmOutage);
            context.execute("SendOutageClearNotificationEmail", SendVmOutageResolvedEmail.class, vmOutageEmailRequest);
        }
    }

    public static class Request extends VmActionRequest {
        public long outageId;
        public VirtualMachine virtualMachine;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }
}
