package com.godaddy.vps4.web.firewall;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
import com.godaddy.vps4.orchestration.firewall.Vps4ClearFirewallCache;
import com.godaddy.vps4.orchestration.firewall.Vps4ModifyFirewallSite;
import com.godaddy.vps4.orchestration.firewall.Vps4RemoveFirewallSite;
import com.godaddy.vps4.orchestration.firewall.Vps4SubmitFirewallCreation;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
    private static final int CDN_SIZE_LIMIT = 5;
    private static final Logger logger = LoggerFactory.getLogger(FirewallResource.class);
    private final VmResource vmResource;
    private final CreditService creditService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;
    private final FirewallService firewallService;
    private final FirewallDataService firewallDataService;
    private final Cryptography cryptography;

    @Inject
    public FirewallResource(GDUser user,
                            VmResource vmResource,
                            CreditService creditService,
                            FirewallService firewallService,
                            FirewallDataService firewallDataService,
                            Cryptography cryptography,
                            ActionService actionService,
                            CommandService commandService) {
        this.user = user;
        this.vmResource = vmResource;
        this.creditService = creditService;
        this.firewallService = firewallService;
        this.firewallDataService = firewallDataService;
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
        validateNoConflictingActions(vmId, actionService, ActionType.DELETE_CDN, ActionType.MODIFY_CDN, ActionType.CREATE_CDN);
    }

    private void validateFirewallSizeLimit(UUID vmId){
        List<VmFirewallSite> sites = firewallDataService.getActiveFirewallSitesOfVm(vmId);
        if(sites != null && sites.size() >= CDN_SIZE_LIMIT) {
            throw new Vps4Exception("SIZE_LIMIT_REACHED", "Vm has reached the maximum quota of allowed CDN sites");
        }
    }

    private void validateFirewallClearCacheConflictingActions(UUID vmId){
        validateNoConflictingActions(vmId, actionService, ActionType.CLEAR_CDN_CACHE);
    }
    @GET
    @Path("/{vmId}/cdn")
    public List<FirewallSite> getActiveFirewallSites(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        return firewallService.getFirewallSites(credit.getShopperId(), getCustomerJwt(), vmId);
    }

    @GET
    @Path("/{vmId}/cdn/{siteId}")
    public FirewallDetail getFirewallSiteDetail(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        return firewallService.getFirewallSiteDetail(credit.getShopperId(), getCustomerJwt(), siteId, vmId);
    }

    @POST
    @Path("/{vmId}/cdn")
    public VmAction createFirewallSite(@PathParam("vmId") UUID vmId, VmCreateFirewallRequest request) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateFirewallSizeLimit(vmId);
        validateFirewallConflictingActions(vmId);

        Vps4SubmitFirewallCreation.Request submitFirewallCreationReq = new Vps4SubmitFirewallCreation.Request();
        submitFirewallCreationReq.domain = request.domain;
        submitFirewallCreationReq.ipAddress = request.ipAddress;
        submitFirewallCreationReq.vmId = vmId;
        submitFirewallCreationReq.shopperId = credit.getShopperId();
        submitFirewallCreationReq.encryptedCustomerJwt = cryptography.encrypt(getCustomerJwt());
        submitFirewallCreationReq.bypassWAF = request.bypassWAF;
        submitFirewallCreationReq.cacheLevel = request.cacheLevel;

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.CREATE_CDN, submitFirewallCreationReq, "Vps4SubmitFirewallCreation", user);
    }

    @DELETE
    @Path("/{vmId}/cdn/{siteId}/cache")
    public VmAction clearFirewallSiteCache(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateFirewallClearCacheConflictingActions(vmId);

        Vps4ClearFirewallCache.Request request = new Vps4ClearFirewallCache.Request();
        request.siteId = siteId;
        request.vmId = vmId;
        request.shopperId = credit.getShopperId();
        request.encryptedCustomerJwt = cryptography.encrypt(getCustomerJwt());

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.CLEAR_CDN_CACHE, request, "Vps4ClearFirewallCache", user);
    }

    @DELETE
    @Path("/{vmId}/cdn/{siteId}")
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
                ActionType.DELETE_CDN,  request, "Vps4RemoveFirewallSite", user);
    }

    @PATCH
    @Path("/{vmId}/cdn/{siteId}")
    public VmAction updateFirewallSite(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId,
                                       VmUpdateFirewallRequest vmUpdateFirewallRequest) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateFirewallConflictingActions(vmId);

        Vps4ModifyFirewallSite.Request request = new Vps4ModifyFirewallSite.Request();
        request.siteId = siteId;
        request.vmId = vmId;
        request.shopperId = credit.getShopperId();
        request.encryptedCustomerJwt = cryptography.encrypt(getCustomerJwt());
        request.bypassWAF = vmUpdateFirewallRequest.bypassWAF;
        request.cacheLevel = vmUpdateFirewallRequest.cacheLevel;

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.MODIFY_CDN,  request, "Vps4ModifyFirewallSite", user);
    }
}
