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
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

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

    @Inject
    public VmMailRelayResource(MailRelayService mailRelayService,
            NetworkService networkService, VmResource vmResource) {
        this.mailRelayService = mailRelayService;
        this.networkService = networkService;
        this.vmResource = vmResource;
    }

    @GET
    @Path("{vmId}/mailRelay/current")
    @ApiOperation(value = "Get today's mail relay use for the selected server", notes = "Get today's mail relay use for the selected server.")
    public MailRelay getCurrentMailRelayUsage(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId) {

        VirtualMachine vm = vmResource.getVm(vmId);
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

}
