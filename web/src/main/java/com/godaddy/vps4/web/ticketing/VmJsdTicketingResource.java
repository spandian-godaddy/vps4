package com.godaddy.vps4.web.ticketing;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jsd.model.JsdCreatedComment;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.jsd.JsdService;
import com.godaddy.vps4.jsd.model.JsdIssueSearchResult;
import com.godaddy.vps4.orchestration.monitoring.CreateJsdOutageTicket;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.vm.VmResource;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.UUID;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmJsdTicketingResource {
    private static final Logger logger = LoggerFactory.getLogger(VmJsdTicketingResource.class);

    private final VmResource vmResource;
    private final JsdService jsdService;
    private final CommandService commandService;
    private final CreditService creditService;

    @Inject
    public VmJsdTicketingResource(VmResource vmResource, JsdService jsdService, CommandService commandService, CreditService creditService) {
        this.jsdService = jsdService;
        this.vmResource = vmResource;
        this.commandService = commandService;
        this.creditService = creditService;
    }

    @POST
    @Path("/{vmId}/ticket")
    @ApiOperation(value = "Create a JSD ticket for a VM",
            notes = "Create a JSD ticket for a VM")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public TicketResponse createTicket(
            @ApiParam(value = "The ID of the server to create ticket for", required = true) @PathParam("vmId") UUID vmId,
            CreateTicketRequest createTicketRequest ) {
        VirtualMachine vm = vmResource.getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        if (!credit.isManaged()) {
            throw new Vps4Exception("INCORRECT_MANAGED_LEVEL","This action is currently only available for fully managed services");
        }

        CreateJsdOutageTicket.Request req = buildCreateRequest(vmId, createTicketRequest);
        CommandState command = Commands.execute(commandService, "CreateJsdOutageTicket", req);
        return new TicketResponse(command.commandId, vmId, command.name, command.responseJson);
    }

    public CreateJsdOutageTicket.Request buildCreateRequest(UUID vmId, CreateTicketRequest createTicketRequest) {
        CreateJsdOutageTicket.Request req = new CreateJsdOutageTicket.Request();
        req.vmId = vmId;
        req.shopperId = createTicketRequest.shopperId;
        req.summary = createTicketRequest.summary;
        req.partnerCustomerKey = createTicketRequest.partnerCustomerKey;
        req.severity = createTicketRequest.severity;
        req.outageId = createTicketRequest.outageId;
        req.metricTypes = createTicketRequest.metricTypes;
        req.metricInfo = createTicketRequest.metricInfo;
        req.metricReasons = createTicketRequest.metricReasons;
        return req;
    }

    public class TicketResponse {
        public UUID commandId;
        public UUID vmId;
        public String commandName;
        public String responseJson;
        public TicketResponse(UUID commandId, UUID vmId, String commandName, String responseJson){
            this.commandId = commandId;
            this.vmId = vmId;
            this.commandName = commandName;
            this.responseJson = responseJson;
        }
    }
    @GET
    @Path("/{vmId}/ticket/{outageId}")
    @ApiOperation(value = "Search for a JSD ticket of an outage for a VM",
            notes = "Search for a JSD ticket of an outage for a VM")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public JsdCreatedIssue searchTicket(
            @ApiParam(value = "The ID of the server to search tickets for", required = true) @PathParam("vmId") UUID vmId,
            @ApiParam(value = "The ID of the server outage to search tickets for", required = true) @PathParam("outageId") Long outageId) {
        logger.info("Searching for JSD ticket for VM {} and outageId {}", vmId, outageId);
        VirtualMachine vm = vmResource.getVm(vmId);

        JsdIssueSearchResult result = jsdService.searchTicket(vm.primaryIpAddress.ipAddress, outageId, vm.orionGuid);
        return (result != null && result.issues != null && !result.issues.isEmpty()) ? result.issues.get(0) : null;
    }

    @POST
    @Path("/{vmId}/ticket/{ticketId}/comment")
    @ApiOperation(value = "Create new comment on JSD ticket for a VM",
            notes = "Create new comment on JSD ticket for a VM")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public JsdCreatedComment commentTicket(
            @ApiParam(value = "The ID of the server to search tickets for", required = true) @PathParam("vmId") UUID vmId,
            @ApiParam(value = "The ID or Key of the ticket to update", required = true) @PathParam("ticketId") String ticketIdOrKey,
            CommentTicketRequest commentTicketRequest) {
        logger.info("Updating JSD ticket for VM {} and ticketId or key {}", vmId, ticketIdOrKey);
        VirtualMachine vm = vmResource.getVm(vmId);

        JsdCreatedComment result = jsdService.commentTicket(ticketIdOrKey, vm.primaryIpAddress.ipAddress,
                commentTicketRequest.items, commentTicketRequest.timestamp);

        return result;
    }

    public static class CreateTicketRequest {
        public String shopperId;
        public String summary;
        public String partnerCustomerKey;
        public String severity;
        public String outageId;
        public String metricTypes;
        public String metricInfo;
        public String metricReasons;
    }


    public static class CommentTicketRequest {
        public String items;
        public Instant timestamp;
    }
}
