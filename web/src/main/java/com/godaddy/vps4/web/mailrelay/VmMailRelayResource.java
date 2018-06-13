package com.godaddy.vps4.web.mailrelay;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.mailrelay.Vps4SetMailRelayQuota;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.StaffOnly;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.vhfs.mailrelay.MailRelay;
import gdg.hfs.vhfs.mailrelay.MailRelayHistory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmMailRelayResource {

    private final MailRelayService mailRelayService;
    private final NetworkService networkService;
    private final VmResource vmResource;
    private final CommandService commandService;
    private final ActionService actionService;
    private final VirtualMachineService virtualMachineService;
    private final GDUser user;

    @Inject
    public VmMailRelayResource(GDUser user, MailRelayService mailRelayService,
            NetworkService networkService, CommandService commandService,
            ActionService actionService, VirtualMachineService virtualMachineService,
            VmResource vmResource) {
        this.mailRelayService = mailRelayService;
        this.networkService = networkService;
        this.commandService = commandService;
        this.actionService = actionService;
        this.virtualMachineService = virtualMachineService;
        this.vmResource = vmResource;
        this.user = user;
    }

    @GET
    @Path("{vmId}/mailRelay/current")
    @ApiOperation(value = "Get today's mail relay use for the selected server", notes = "Get today's mail relay use for the selected server.")
    public MailRelay getCurrentMailRelayUsage(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId) {

        vmResource.getVm(vmId);
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        return mailRelayService.getMailRelay(ipAddress.ipAddress);
    }

    @GET
    @Path("{vmId}/mailRelay/history")
    @ApiOperation(value = "Get past mail relay use for the selected server.  Ordered by date descending.", notes = "Get past mail relay use for the selected server.  Ordered by date descending.")
    public List<MailRelayHistory> getMailRelayHistory(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId,
            @ApiParam(value = "The number of data points to return", defaultValue = "30", required = false) @QueryParam("daysToReturn") int daysToReturn) {
        VirtualMachine vm = vmResource.getVm(vmId);
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        LocalDate startDate = getLocalDateFromInstant(vm.validOn);
        List<MailRelayHistory> history;
        if(daysToReturn > 0){
            history = mailRelayService.getMailRelayHistory(ipAddress.ipAddress, startDate, daysToReturn);
        }
        else{
            history = mailRelayService.getMailRelayHistory(ipAddress.ipAddress, startDate);
        }
        return history;
    }

    private LocalDate getLocalDateFromInstant(Instant i){
        LocalDateTime ldt = LocalDateTime.ofInstant(i, ZoneOffset.UTC);
        return LocalDate.of(ldt.getYear(), ldt.getMonth(), ldt.getDayOfMonth());

    }

    public static class MailRelayQuotaPatch {
        public int quota;
    }

    @StaffOnly
    @PATCH
    @Path("{vmId}/mailRelay")
    @Produces({ "application/json" })
    @ApiOperation(httpMethod = "PATCH",
                  value = "Reset the mail relay quota for the primary IP of the given vm",
                  notes = "Reset the mail relay quota for the primary IP of the given vm")
    public Action updateMailRelayQuota(@ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId,
            MailRelayQuotaPatch quotaPatch) {
        vmResource.getVm(vmId);
        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        Vps4SetMailRelayQuota.Request request = new Vps4SetMailRelayQuota.Request(ipAddress.ipAddress, quotaPatch.quota);
        long actionId = actionService.createAction(vmId, ActionType.UPDATE_MAILRELAY_QUOTA, request.toJSONString(), vps4UserId, user.getUsername());
        request.actionId = actionId;
        Commands.execute(commandService, actionService, "Vps4SetMailRelayQuota", request);
        return actionService.getAction(actionId);
    }

}
