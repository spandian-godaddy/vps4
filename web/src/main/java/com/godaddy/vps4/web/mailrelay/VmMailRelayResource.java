package com.godaddy.vps4.web.mailrelay;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
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

    private final GDUser user;
    private final MailRelayService mailRelayService;
    private final NetworkService networkService;
    private final PrivilegeService privilegeService;
    private final Vps4UserService vps4UserService;

    @Inject
    public VmMailRelayResource(GDUser user, MailRelayService mailRelayService,
            NetworkService networkService, PrivilegeService privilegeService,
            Vps4UserService vps4UserService) {
        this.user = user;
        this.mailRelayService = mailRelayService;
        this.networkService = networkService;
        this.privilegeService = privilegeService;
        this.vps4UserService = vps4UserService;
    }

    private void verifyUserPrivilege(UUID vmId) {
        Vps4User vps4User = vps4UserService.getOrCreateUserForShopper(user.getShopperId());
        privilegeService.requireAnyPrivilegeToVmId(vps4User, vmId);
    }

    @GET
    @Path("{vmId}/mailRelay/current")
    @ApiOperation(value = "Get today's mail relay use for the selected server", notes = "Get today's mail relay use for the selected server.")
    public MailRelay getCurrentMailRelayUsage(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId) {

        if (user.isShopper())
            verifyUserPrivilege(vmId);
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        return mailRelayService.getMailRelay(ipAddress.ipAddress);
    }

    @GET
    @Path("{vmId}/mailRelay/history")
    @ApiOperation(value = "Get past mail relay use for the selected server", notes = "Get past mail relay use for the selected server")
    public List<MailRelayHistory> getMailRelayHistory(
            @ApiParam(value = "The ID of the selected server", required = true) @PathParam("vmId") UUID vmId) {

        if (user.isShopper())
            verifyUserPrivilege(vmId);
        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        return mailRelayService.getMailRelayHistory(ipAddress.ipAddress);
    }

}
