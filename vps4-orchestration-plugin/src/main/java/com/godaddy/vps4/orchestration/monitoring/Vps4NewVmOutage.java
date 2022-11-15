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
        name = "Vps4NewVmOutage",
        requestType = Vps4NewVmOutage.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4NewVmOutage extends ActionCommand<Vps4NewVmOutage.Request, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4NewVmOutage.class);

    private final CreditService creditService;
    private CommandContext context;
    private final Config config;

    @Inject
    public Vps4NewVmOutage(ActionService actionService, CreditService creditService, Config config) {
        super(actionService);
        this.creditService = creditService;
        this.config = config;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;
        if(request.virtualMachine.isCanceledOrDeleted())
        {
            logger.info("VM {} is not active, no need to create outage {}", request.virtualMachine.vmId, request.outageId);
            return null;
        }
        VmOutage outage = getVmOutage(request);
        logger.info("New outage {} reported for VM {}", request.outageId, request.virtualMachine.vmId);
        sendOutageNotificationEmail(request.virtualMachine, outage);
        executeCreateJsdTicket(request.virtualMachine, outage, request.partnerCustomerKey);
        return null;
    }

    private VmOutage getVmOutage(Request request) {
        GetPanoptaOutage.Request getOutageRequest = new GetPanoptaOutage.Request();
        getOutageRequest.vmId = request.virtualMachine.vmId;
        getOutageRequest.outageId = request.outageId;
        VmOutage outage = context.execute("GetPanoptaOutage", GetPanoptaOutage.class, getOutageRequest);
        return outage;
    }

    private void executeCreateJsdTicket(VirtualMachine virtualMachine, VmOutage outage, String partnerCustomerKey) {
        boolean shouldCreateJsdTicket = Boolean.parseBoolean(config.get("jsd.enabled", "false"));
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);

        if (credit != null && credit.isAccountActive() && virtualMachine.isActive()
                && credit.isManaged()
                && shouldCreateJsdTicket) {
            createJsdTicket(virtualMachine, credit.getShopperId(), outage, partnerCustomerKey);
        }
    }

    private void sendOutageNotificationEmail(VirtualMachine virtualMachine, VmOutage vmOutage) {
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);
        if (credit != null && credit.isAccountActive() && virtualMachine.isActive() && !credit.isManaged()) {
            VmOutageEmailRequest vmOutageEmailRequest =
                    new VmOutageEmailRequest(virtualMachine.name, virtualMachine.primaryIpAddress.ipAddress,
                            credit.getOrionGuid(), credit.getShopperId(), virtualMachine.vmId, credit.isManaged(),
                            vmOutage);
            context.execute("SendOutageNotificationEmail", SendVmOutageEmail.class, vmOutageEmailRequest);
        }
    }


    private void createJsdTicket(VirtualMachine virtualMachine, String shopperId, VmOutage vmOutage, String partnerCustomerKey) {
        String metricType = vmOutage.metricTypeMapper();
        CreateJsdOutageTicket.Request createJsdOutageTicketRequest = new CreateJsdOutageTicket.Request();
        createJsdOutageTicketRequest.vmId = virtualMachine.vmId;
        createJsdOutageTicketRequest.shopperId = shopperId;
        createJsdOutageTicketRequest.summary = "Monitoring Event - " + vmOutage.metrics.toString() + " - " +
                vmOutage.reason + " (" + vmOutage.panoptaOutageId + ")";
        createJsdOutageTicketRequest.partnerCustomerKey = partnerCustomerKey;
        createJsdOutageTicketRequest.severity = vmOutage.severity;
        createJsdOutageTicketRequest.outageId = Long.toString(vmOutage.panoptaOutageId);
        createJsdOutageTicketRequest.metricTypes = metricType;
        createJsdOutageTicketRequest.metricInfo = metricType;
        createJsdOutageTicketRequest.metricReasons = vmOutage.reason;
        context.execute("CreateJsdOutageTicket", CreateJsdOutageTicket.class, createJsdOutageTicketRequest);
    }
    
    public static class Request extends VmActionRequest {
        public long outageId;
        public String partnerCustomerKey;
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
