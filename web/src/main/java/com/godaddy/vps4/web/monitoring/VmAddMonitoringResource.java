package com.godaddy.vps4.web.monitoring;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.monitoring.Vps4AddDomainMonitoring;
import com.godaddy.vps4.orchestration.panopta.Vps4RemoveDomainMonitoring;
import com.godaddy.vps4.orchestration.panopta.Vps4ReplaceDomainMonitoring;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaMetricId;
import com.godaddy.vps4.panopta.PanoptaMetricMapper;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmAddMonitoringResource {
    private static final Logger logger = LoggerFactory.getLogger(VmAddMonitoringResource.class);

    private final GDUser user;
    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;
    private final CreditService creditService;
    private final PanoptaDataService panoptaDataService;
    private final PanoptaService panoptaService;
    private final PanoptaMetricMapper panoptaMetricMapper;

    private static final long MANAGED_DOMAINS_LIMIT = 5;
    private static final long SELF_MANAGED_DOMAINS_LIMIT = 1;

    @Inject
    public VmAddMonitoringResource(GDUser user,
                                   VmResource vmResource,
                                   ActionService actionService,
                                   CommandService commandService,
                                   CreditService creditService,
                                   PanoptaDataService panoptaDataService,
                                   PanoptaService panoptaService,
                                   PanoptaMetricMapper panoptaMetricMapper) {
        this.user = user;
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        this.creditService = creditService;
        this.panoptaDataService = panoptaDataService;
        this.panoptaService = panoptaService;
        this.panoptaMetricMapper = panoptaMetricMapper;
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

    private boolean isManagedDomainLimitReached(boolean isManaged, List<String> fqdns) {
        int activeDomainsCount = fqdns.size();
        return isManaged? ( activeDomainsCount >= MANAGED_DOMAINS_LIMIT ) : activeDomainsCount >= SELF_MANAGED_DOMAINS_LIMIT;
    }

    private boolean isFqdnDuplicate(String additionalFqdn, List<String> fqdns) {
        return fqdns.contains(additionalFqdn);
    }

    private void validateFqdnConflictingActions(UUID vmId){
        validateNoConflictingActions(vmId, actionService, ActionType.ADD_MONITORING, ActionType.DELETE_DOMAIN_MONITORING,
                ActionType.ADD_DOMAIN_MONITORING, ActionType.REPLACE_DOMAIN_MONITORING);
    }

    @POST
    @Path("/{vmId}/monitoring/additionalFqdn")
    @ApiOperation(value = "Add domain to monitoring on customer server",
            notes = "overrideProtocol field only accepts 'HTTP' or 'HTTPS' values")
    public VmAction addDomainToMonitoring(@PathParam("vmId") UUID vmId,
                                          AddDomainToMonitoringRequest addDomainMonitoringRequest) {
        if(addDomainMonitoringRequest.additionalFqdn == null ) {
            throw new Vps4Exception("INVALID_ADDITIONAL_FQDN", "Additional fqdn field cannot be empty.");
        }
        VirtualMachine vm = vmResource.getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        List<String> activeFqdns = panoptaDataService.getPanoptaActiveAdditionalFqdns(vmId);

        if(this.isManagedDomainLimitReached(credit.isManaged(), activeFqdns)) {
            throw new Vps4Exception("DOMAIN_LIMIT_REACHED", "Domain limit has been reached on this server.");
        }
        if(this.isFqdnDuplicate(addDomainMonitoringRequest.additionalFqdn, activeFqdns)) {
            throw new Vps4Exception("DUPLICATE_FQDN", "This server already has an active entry for fqdn: " + addDomainMonitoringRequest.additionalFqdn );
        }

        validateFqdnConflictingActions(vmId);

        Vps4AddDomainMonitoring.Request request = new Vps4AddDomainMonitoring.Request();
        request.overrideProtocol = addDomainMonitoringRequest.overrideProtocol == null ?
                null : addDomainMonitoringRequest.overrideProtocol.toString();
        request.vmId = vmId;
        request.additionalFqdn = addDomainMonitoringRequest.additionalFqdn;
        request.osTypeId = vm.image.operatingSystem.getOperatingSystemId();
        request.isManaged = credit.isManaged();
        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.ADD_DOMAIN_MONITORING, request, "Vps4AddDomainMonitoring", user);
    }

    @DELETE
    @Path("/{vmId}/monitoring/additionalFqdn/{additionalFqdn}")
    @ApiOperation(value = "delete domain monitoring on customer server",
            notes = "protocol field only accepts 'HTTP' or 'HTTPS' values")
    public VmAction deleteDomainMonitoring(@PathParam("vmId") UUID vmId, @PathParam("additionalFqdn") String additionalFqdn) {
        validateFqdnConflictingActions(vmId);
        Vps4RemoveDomainMonitoring.Request request = new Vps4RemoveDomainMonitoring.Request();
        request.vmId = vmId;
        request.additionalFqdn = additionalFqdn;
        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.DELETE_DOMAIN_MONITORING,  request, "Vps4RemoveDomainMonitoring", user);
    }

    @PUT
    @Path("/{vmId}/monitoring/additionalFqdn")
    @ApiOperation(value = "replace HTTP/HTTPS domain monitoring on customer server")
    public VmAction replaceDomainMonitoring(@PathParam("vmId") UUID vmId,
                                            ReplaceDomainToMonitoringRequest replaceDomainToMonitoringRequest) throws PanoptaServiceException {
        if (replaceDomainToMonitoringRequest.protocol == null) {
            throw new Vps4Exception("PROTOCOL_INVALID", "Protocol received is null.");
        }

        validateFqdnConflictingActions(vmId);

        PanoptaMetricId metric =
                panoptaService.getNetworkIdOfAdditionalFqdn(vmId, replaceDomainToMonitoringRequest.additionalFqdn);

        if (metric == null) {
            throw new Vps4Exception("METRIC_NOT_FOUND", "Panopta metric was not found.");
        }
        if (replaceDomainToMonitoringRequest.protocol.toString() == panoptaMetricMapper.getVmMetric(metric.typeId).toString()) {
            logger.info("Requested protocol matches existing protocol for vmId {} - no further action for metric Id {} ", vmId, metric.id);
            return null;
        }

        VirtualMachine vm = vmResource.getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        Vps4ReplaceDomainMonitoring.Request request = new Vps4ReplaceDomainMonitoring.Request();
        request.vmId = vmId;
        request.additionalFqdn = replaceDomainToMonitoringRequest.additionalFqdn;
        request.operatingSystemId = vm.image.operatingSystem.getOperatingSystemId();
        request.isManaged = credit.isManaged();
        request.protocol = replaceDomainToMonitoringRequest.protocol.toString();
        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.REPLACE_DOMAIN_MONITORING,  request, "Vps4ReplaceDomainMonitoring", user);
    }

    public enum FqdnProtocol {
        HTTP, HTTPS
    }

    public static class AddDomainToMonitoringRequest {
        public String additionalFqdn;
        public FqdnProtocol overrideProtocol;
    }

    public static class ReplaceDomainToMonitoringRequest {
        public String additionalFqdn;
        public FqdnProtocol protocol;
    }
}
