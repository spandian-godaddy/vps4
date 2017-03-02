package com.godaddy.vps4.web.mailrelay;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.web.Vps4Api;
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
    private final PrivilegeService privilegeService;
    private final Vps4User user;
    
    @Inject
    public VmMailRelayResource(Vps4User user, MailRelayService mailRelayService, NetworkService networkService,
            PrivilegeService privilegeService) {
        this.user = user;
        this.mailRelayService = mailRelayService;
        this.networkService = networkService;
        this.privilegeService = privilegeService;
    }

    @GET
    @Path("{vmId}/mailRelay/current")
    @ApiOperation(value = "Get today's mail relay use for the selected server", notes = "Get today's mail relay use for the selected server.")
    public MailRelay getCurrentMailRelayUsage(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId, 
            @ApiParam(value = "Force refresh of the mail relay usage cache", required = false) @DefaultValue("false") @QueryParam("forceRefresh") boolean forceRefresh) {
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        return mailRelayService.getMailRelay(ipAddress.ipAddress, forceRefresh);
    }
    
    @GET
    @Path("{vmId}/mailRelay/history")
    @ApiOperation(value = "Get past mail relay use for the selected server", notes = "Get past mail relay use for the selected server")
    public List<MailRelayHistory> getMailRelayHistory(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId,
            @ApiParam(value = "Force refresh of the mail relay history cache", required = true) @DefaultValue("false") @QueryParam("forceRefresh") boolean forceRefresh) {
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        return mailRelayService.getMailRelayHistory(ipAddress.ipAddress, forceRefresh);
    }
    
}
