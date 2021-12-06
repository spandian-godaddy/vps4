package com.godaddy.vps4.web.monitoring;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddDomainMonitoring;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmAddMonitoringResource {

    private final GDUser user;
    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;
    private final CreditService creditService;
    private final PanoptaDataService panoptaDataService;
    private static final long MANAGED_DOMAINS_LIMIT = 5;
    private static final long SELF_MANAGED_DOMAINS_LIMIT = 1;

    @Inject
    public VmAddMonitoringResource(GDUser user,
                                   VmResource vmResource,
                                   ActionService actionService,
                                   CommandService commandService,
                                   CreditService creditService,
                                   PanoptaDataService panoptaDataService) {
        this.user = user;
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        this.creditService = creditService;
        this.panoptaDataService = panoptaDataService;
    }

    @POST
    @Path("/{vmId}/monitoring/install")
    @ApiOperation(value = "Install monitoring agent on customer server")
    public VmAction installPanoptaAgent(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        validateServerIsActive(vmResource.getVmFromVmVertical(vm.hfsVmId));
        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                ActionType.RESTART_VM, ActionType.ADD_MONITORING);

        VmActionRequest request = new VmActionRequest();
        request.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.ADD_MONITORING, request, "Vps4AddMonitoring", user);
    }

    private boolean isManagedDomainLimitReached(boolean isManaged, UUID vmId) {
        int activeDomainsCount = panoptaDataService.getPanoptaActiveAdditionalFqdns(vmId).size();
        return isManaged? ( activeDomainsCount >= MANAGED_DOMAINS_LIMIT ) : activeDomainsCount >= SELF_MANAGED_DOMAINS_LIMIT;
    }

    @POST
    @Path("/{vmId}/monitoring/additionalFqdn")
    @ApiOperation(value = "Add domain to monitoring on customer server")
    public VmAction addDomainToMonitoring(@PathParam("vmId") UUID vmId,
                                          AddDomainToMonitoringRequest addDomainMonitoringRequest) {
        if(addDomainMonitoringRequest.additionalFqdn == null ) {
            throw new Vps4Exception("INVALID_ADDITIONAL_FQDN", "Additional fqdn field cannot be empty.");
        }
        VirtualMachine vm = vmResource.getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        if(this.isManagedDomainLimitReached(credit.isManaged(), vm.vmId)) {
            throw new Vps4Exception("DOMAIN_LIMIT_REACHED", "Domain limit has been reached on this server.");
        }

        validateNoConflictingActions(vmId, actionService, ActionType.ADD_MONITORING, ActionType.ADD_DOMAIN_MONITORING);

        Vps4AddDomainMonitoring.Request request = new Vps4AddDomainMonitoring.Request();
        request.vmId = vmId;
        request.additionalFqdn = addDomainMonitoringRequest.additionalFqdn;
        request.osTypeId = vm.image.operatingSystem.getOperatingSystemId();
        request.isManaged = credit.isManaged();
        request.hasMonitoring = credit.hasMonitoring();
        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.ADD_DOMAIN_MONITORING, request, "Vps4AddDomainMonitoring", user);
    }


    public static class AddDomainToMonitoringRequest {
        public String additionalFqdn;
    }
}
