package com.godaddy.vps4.web.firewall;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.orchestration.firewall.Vps4RemoveFirewallSite;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FirewallResource {

    private static final Logger logger = LoggerFactory.getLogger(FirewallResource.class);

    private final VmResource vmResource;
    private final CreditService creditService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;
    private final FirewallService firewallService;

    private final Cryptography cryptography;

    @Inject
    public FirewallResource(GDUser user,
                            VmResource vmResource,
                            CreditService creditService,
                            FirewallService firewallService,
                            Cryptography cryptography,
                            ActionService actionService,
                            CommandService commandService) {
        this.user = user;
        this.vmResource = vmResource;
        this.creditService = creditService;
        this.firewallService = firewallService;
        this.cryptography = cryptography;
        this.actionService = actionService;
        this.commandService = commandService;
    }

    private String getCustomerJwt() {
        if (user.isShopper()) {
            return user.getToken().getJwt().getParsedString();
        }
        return null;
    }

    private void validateFirewallConflictingActions(UUID vmId){
        validateNoConflictingActions(vmId, actionService, ActionType.DELETE_FIREWALL);
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

    @DELETE
    @Path("/{vmId}/firewallSites/{siteId}")
    public VmAction deleteFirewallSite(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateFirewallConflictingActions(vmId);

        Vps4RemoveFirewallSite.Request request = new Vps4RemoveFirewallSite.Request();
        request.siteId = siteId;
        request.vmId = vmId;
        request.shopperId = credit.getShopperId();
        request.encryptedCustomerJwt = cryptography.encrypt(getCustomerJwt());

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.DELETE_FIREWALL,  request, "Vps4RemoveFirewallSite", user);
    }
}
