package com.godaddy.vps4.web.ticketing;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.jsd.model.CreateJsdTicketRequest;
import com.godaddy.vps4.jsd.model.JsdCreatedComment;
import com.godaddy.vps4.jsd.model.JsdCreatedIssue;
import com.godaddy.vps4.jsd.JsdService;
import com.godaddy.vps4.jsd.model.JsdIssueSearchResult;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
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
    private static final String INCIDENT_URL_PREFIX = "https://my.panopta.com/outage/manageIncident?incident_id=";
    private static final Logger logger = LoggerFactory.getLogger(VmJsdTicketingResource.class);

    private final VmResource vmResource;
    private final JsdService jsdService;
    private final CreditService creditService;

    @Inject
    public VmJsdTicketingResource(VmResource vmResource, JsdService jsdService, CreditService creditService) {
        this.jsdService = jsdService;
        this.vmResource = vmResource;
        this.creditService = creditService;
    }

    @POST
    @Path("/{vmId}/ticket")
    @ApiOperation(value = "Create a JSD ticket for a VM",
            notes = "Create a JSD ticket for a VM")
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    public JsdCreatedIssue createTicket(
            @ApiParam(value = "The ID of the server to create ticket for", required = true) @PathParam("vmId") UUID vmId,
            CreateTicketRequest createTicketRequest ) {
        logger.info("Creating JSD ticket for VM {}", vmId);
        VirtualMachine vm = vmResource.getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        String managedLevel = managedLevelMapper(credit.isManaged());
        if (StringUtils.isEmpty(managedLevel)) {
            throw new Vps4Exception("NOT_ALLOWED_FOR_SELF_MANAGED", "This action is currently only available for fully managed services");
        }
        CreateJsdTicketRequest request = new CreateJsdTicketRequest();
        request.orionGuid = vm.orionGuid.toString();
        request.shopperId = createTicketRequest.shopperId;
        request.summary = createTicketRequest.summary;
        request.partnerCustomerKey = createTicketRequest.partnerCustomerKey;
        request.plid = credit.getResellerId();
        request.fqdn = vm.primaryIpAddress.ipAddress;
        request.severity = createTicketRequest.severity;
        request.outageId = createTicketRequest.outageId;
        request.outageIdUrl = INCIDENT_URL_PREFIX + createTicketRequest.outageId;
        request.metricTypes = createTicketRequest.metricTypes;
        request.dataCenter = dataCenterMapper(vm.dataCenter.dataCenterId);
        request.metricInfo = createTicketRequest.metricInfo;
        request.metricReasons = createTicketRequest.metricReasons;
        request.supportProduct = serverTypeMapper(credit.isDed4());
        request.customerProduct = managedLevel;

        return jsdService.createTicket(request);
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
    public String managedLevelMapper(boolean managed) {
        if (managed) {
                return "Fully Managed";
        }
        return null; // currently, JSD tickets is only implemented for fully managed VMs
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
