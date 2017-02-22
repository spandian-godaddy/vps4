package com.godaddy.vps4.web.smtp;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import gdg.hfs.vhfs.mailrelay.MailRelay;
import gdg.hfs.vhfs.mailrelay.MailRelayHistory;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmSmtpResource {

    private final MailRelayService mailRelayService;
    private final NetworkService networkService;
    private final PrivilegeService privilegeService;
    private final Vps4User user;
    
    @Inject
    public VmSmtpResource(Vps4User user, MailRelayService mailRelayService, NetworkService networkService,
            PrivilegeService privilegeService) {
        this.user = user;
        this.mailRelayService = mailRelayService;
        this.networkService = networkService;
        this.privilegeService = privilegeService;
    }

    @GET
    @Path("{vmId}/smtp/current")
    public MailRelay getCurrentSmtpUsage(@PathParam("vmId") UUID vmId) {
        
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);

        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        return mailRelayService.getMailRelay(ipAddress.ipAddress);
    }
    
    @GET
    @Path("{vmId}/smtp/history")
    public List<MailRelayHistory> getSmtpUsageHistory(@PathParam("vmId") UUID vmId) {

        privilegeService.requireAnyPrivilegeToVmId(user, vmId);

        IpAddress ipAddress = networkService.getVmPrimaryAddress(vmId);
        return mailRelayService.getRelayHistory(ipAddress.ipAddress);
    }
    
}
