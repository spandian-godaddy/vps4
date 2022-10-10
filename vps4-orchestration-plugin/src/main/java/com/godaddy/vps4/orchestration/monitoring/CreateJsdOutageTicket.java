package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jsd.JsdService;
import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

@CommandMetadata(
        name = "CreateJsdOutageTicket",
        requestType = CreateJsdOutageTicket.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class CreateJsdOutageTicket implements Command<CreateJsdOutageTicket.Request, JsdCreatedIssue> {
    private static final String INCIDENT_URL_PREFIX = "https://my.panopta.com/outage/manageIncident?incident_id=";

    private static final Logger logger = LoggerFactory.getLogger(CreateJsdOutageTicket.class);

    private final VirtualMachineService vmService;
    private final JsdService jsdService;
    private final CreditService creditService;

    @Inject
    public CreateJsdOutageTicket(VirtualMachineService vmService, JsdService jsdService, CreditService creditService) {
        this.vmService = vmService;
        this.jsdService = jsdService;
        this.creditService = creditService;
    }

    @Override
    public JsdCreatedIssue execute(CommandContext commandContext, CreateJsdOutageTicket.Request request) {
        logger.info("Creating JSD ticket for VM {}", request.vmId);
        VirtualMachine vm = vmService.getVirtualMachine(request.vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        CreateJsdTicketRequest req = new CreateJsdTicketRequest();
        req.orionGuid = vm.orionGuid.toString();
        req.shopperId = request.shopperId;
        req.summary = request.summary;
        req.partnerCustomerKey = request.partnerCustomerKey;
        req.plid = credit.getResellerId();
        req.fqdn = vm.primaryIpAddress.ipAddress;
        req.severity = request.severity;
        req.outageId = request.outageId;
        req.outageIdUrl = INCIDENT_URL_PREFIX + request.outageId;
        req.metricTypes = request.metricTypes;
        req.dataCenter = dataCenterMapper(vm.dataCenter.dataCenterId);
        req.metricInfo = request.metricInfo;
        req.metricReasons = request.metricReasons;
        req.supportProduct = serverTypeMapper(credit.isDed4());
        req.customerProduct = managedLevelMapper(credit.isManaged());

        return jsdService.createTicket(req);
    }

    public String managedLevelMapper(boolean managed) {
        if (managed) {
            return "Fully Managed";
        }
        return null;
    }

    public String serverTypeMapper(boolean isDed4) {
        if (isDed4) {
            return "ded4";
        }
        return "vps4";
    }

    public String dataCenterMapper(int dcId) {
        switch (dcId) {
            case 1:
                return "p3";
            case 2:
                return "a2";
            case 4:
                return "n3";
            case 3:
            case 5:
                return "sg2";
            default:
                return null;
        }
    }

    public static class Request {
        public UUID vmId;
        public String shopperId;
        public String summary;
        public String partnerCustomerKey;
        public String severity;
        public String outageId;
        public String metricTypes;
        public String metricInfo;
        public String metricReasons;
    }

}
