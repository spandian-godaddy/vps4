package com.godaddy.vps4.web.firewall;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FirewallResource {

    private static final Logger logger = LoggerFactory.getLogger(FirewallResource.class);

    private final VmResource vmResource;
    private final CreditService creditService;
    private final GDUser user;
    private final FirewallService firewallService;

    @Inject
    public FirewallResource(GDUser user,
                            VmResource vmResource,
                            CreditService creditService,
                            FirewallService firewallService) {
        this.user = user;
        this.vmResource = vmResource;
        this.creditService = creditService;
        this.firewallService = firewallService;
    }

    private String getCustomerJwt() {
        if (user.isShopper()) {
            return user.getToken().getJwt().getParsedString();
        }
        return null;
    }

    @GET
    @Path("/{vmId}/firewallSites")
    public List<FirewallSite> getActiveFirewallSites(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        return firewallService.getFirewallSites(credit.getShopperId(), getCustomerJwt(), vmId);
    }

    @GET
    @Path("/{vmId}/firewallSites/{siteId}")
    public FirewallDetail getFirewallSiteDetail(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        return firewallService.getFirewallSiteDetail(credit.getShopperId(), getCustomerJwt(), siteId, vmId);
    }
}
