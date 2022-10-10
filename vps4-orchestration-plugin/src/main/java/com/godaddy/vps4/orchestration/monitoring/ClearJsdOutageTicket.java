package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.jsd.JsdService;
import com.godaddy.vps4.jsd.model.JsdCreatedComment;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.jsd.model.JsdIssueSearchResult;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

@CommandMetadata(
        name = "ClearJsdOutageTicket",
        requestType = ClearJsdOutageTicket.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class ClearJsdOutageTicket implements Command<ClearJsdOutageTicket.Request, JsdCreatedComment> {
    private static final Logger logger = LoggerFactory.getLogger(ClearJsdOutageTicket.class);

    private final VirtualMachineService vmService;
    private final JsdService jsdService;

    @Inject
    public ClearJsdOutageTicket(VirtualMachineService vmService, JsdService jsdService) {
        this.vmService = vmService;
        this.jsdService = jsdService;
    }

    @Override
    public JsdCreatedComment execute(CommandContext commandContext, ClearJsdOutageTicket.Request request) {
        VirtualMachine vm = vmService.getVirtualMachine(request.vmId);
        JsdCreatedIssue ticket = findTicket(vm.primaryIpAddress.ipAddress, request.outageId, vm.orionGuid);
        if (ticket == null) {
            logger.info("ticket for outage id {} not found. No longer attempting to update JSD ticket", request.outageId);
            return null;
        }
        logger.info("Updating JSD ticket {} for outage {}" , ticket.issueKey, request.outageId);
        return jsdService.commentTicket(ticket.issueKey, vm.primaryIpAddress.ipAddress, request.outageMetrics, request.outageTimestamp);
    }

    private JsdCreatedIssue findTicket(String ipAddress, long outageId, UUID orionGuid){
        JsdIssueSearchResult result = jsdService.searchTicket(ipAddress, outageId, orionGuid);
        return (result != null && result.issues != null && !result.issues.isEmpty()) ? result.issues.get(0) : null;
    }

    public static class Request {
        public UUID vmId;
        public long outageId;
        public String outageMetrics;
        public Instant outageTimestamp;
    }
}
