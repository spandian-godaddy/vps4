package com.godaddy.vps4.web.cdn;

import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnSite;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.orchestration.cdn.Vps4ClearCdnCache;
import com.godaddy.vps4.orchestration.cdn.Vps4ModifyCdnSite;
import com.godaddy.vps4.orchestration.cdn.Vps4RemoveCdnSite;
import com.godaddy.vps4.orchestration.cdn.Vps4SubmitCdnCreation;
import com.godaddy.vps4.orchestration.cdn.Vps4ValidateCdn;
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
import java.util.stream.Collectors;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CdnResource {
    private static final Logger logger = LoggerFactory.getLogger(CdnResource.class);
    private final VmResource vmResource;
    private final CreditService creditService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;
    private final CdnService cdnService;
    private final CdnDataService cdnDataService;

    @Inject
    public CdnResource(GDUser user,
                       VmResource vmResource,
                       CreditService creditService,
                       CdnService cdnService,
                       CdnDataService cdnDataService,
                       ActionService actionService,
                       CommandService commandService) {
        this.user = user;
        this.vmResource = vmResource;
        this.creditService = creditService;
        this.cdnService = cdnService;
        this.cdnDataService = cdnDataService;
        this.actionService = actionService;
        this.commandService = commandService;
    }

    private void validateCdnConflictingActions(UUID vmId){
        validateNoConflictingActions(vmId, actionService, ActionType.DELETE_CDN, ActionType.MODIFY_CDN, 
        ActionType.CREATE_CDN, ActionType.VALIDATE_CDN);
    }

    private void validateCdnSizeLimit(List<VmCdnSite> sites, int cdnSizeLimit){
        if (sites != null) {
            if (sites.size() >= cdnSizeLimit) {
                throw new Vps4Exception("SIZE_LIMIT_REACHED", "Vm has reached the maximum quota of allowed CDN sites");
            }
        }
    }

    private void validateCdnDomainDuplicates(List<VmCdnSite> sites, String domain){
        if (sites != null) {
            List<VmCdnSite> filteredDuplicates = sites.stream().filter(cdnSite ->
                    domain.toLowerCase().equals(cdnSite.domain.toLowerCase())).collect(Collectors.toList());
            if (filteredDuplicates != null && !filteredDuplicates.isEmpty()) {
                throw new Vps4Exception("DUPLICATE_DOMAIN", "Vm has another active CDN site with the same domain name");
            }
        }
    }

    private void validateCdnClearCacheConflictingActions(UUID vmId){
        validateNoConflictingActions(vmId, actionService, ActionType.CLEAR_CDN_CACHE);
    }
    @GET
    @Path("/{vmId}/cdn")
    public List<CdnSite> getActiveCdnSites(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        return cdnService.getCdnSites(credit.getCustomerId(), vmId);
    }

    @GET
    @Path("/{vmId}/cdn/{siteId}")
    public CdnDetail getCdnSiteDetail(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        return cdnService.getCdnSiteDetail(credit.getCustomerId(), siteId, vmId);
    }

    @POST
    @Path("/{vmId}/cdn")
    public VmAction createCdnSite(@PathParam("vmId") UUID vmId, VmCreateCdnRequest request) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        List<VmCdnSite> sites = cdnDataService.getActiveCdnSitesOfVm(vmId);
        validateCdnSizeLimit(sites, credit.entitlementData.cdnWaf);
        validateCdnDomainDuplicates(sites, request.domain);
        validateCdnConflictingActions(vmId);

        Vps4SubmitCdnCreation.Request submitCdnCreationReq = new Vps4SubmitCdnCreation.Request();
        submitCdnCreationReq.domain = request.domain;
        submitCdnCreationReq.ipAddress = request.ipAddress;
        submitCdnCreationReq.vmId = vmId;
        submitCdnCreationReq.customerId = credit.getCustomerId();
        submitCdnCreationReq.bypassWAF = request.bypassWAF;
        submitCdnCreationReq.cacheLevel = validateAndReturnEnumValue(CdnCacheLevel.class, request.cacheLevel);

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.CREATE_CDN, submitCdnCreationReq, "Vps4SubmitCdnCreation", user);
    }

    @POST
    @Path("/{vmId}/cdn/{siteId}/validation")
    public VmAction validateCdnSite(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateCdnConflictingActions(vmId);

        Vps4ValidateCdn.Request validateCdnReq = new Vps4ValidateCdn.Request();
        validateCdnReq.siteId = siteId;
        validateCdnReq.vmId = vmId;
        validateCdnReq.customerId = credit.getCustomerId();

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.VALIDATE_CDN, validateCdnReq, "Vps4ValidateCdn", user);
    }

    @DELETE
    @Path("/{vmId}/cdn/{siteId}/cache")
    public VmAction clearCdnSiteCache(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateCdnClearCacheConflictingActions(vmId);

        Vps4ClearCdnCache.Request request = new Vps4ClearCdnCache.Request();
        request.siteId = siteId;
        request.vmId = vmId;
        request.customerId = credit.getCustomerId();

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.CLEAR_CDN_CACHE, request, "Vps4ClearCdnCache", user);
    }

    @DELETE
    @Path("/{vmId}/cdn/{siteId}")
    public VmAction deleteCdnSite(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateCdnConflictingActions(vmId);

        Vps4RemoveCdnSite.Request request = new Vps4RemoveCdnSite.Request();
        request.siteId = siteId;
        request.vmId = vmId;
        request.customerId = credit.getCustomerId();

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.DELETE_CDN,  request, "Vps4RemoveCdnSite", user);
    }

    @PATCH
    @Path("/{vmId}/cdn/{siteId}")
    public VmAction updateCdnSite(@PathParam("vmId") UUID vmId, @PathParam("siteId") String siteId,
                                  VmUpdateCdnRequest vmUpdateCdnRequest) {
        VirtualMachine vm = vmResource.getVm(vmId);  // auth validation
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);

        validateCdnConflictingActions(vmId);

        Vps4ModifyCdnSite.Request request = new Vps4ModifyCdnSite.Request();
        request.siteId = siteId;
        request.vmId = vmId;
        request.customerId = credit.getCustomerId();
        request.bypassWAF = vmUpdateCdnRequest.bypassWAF;
        request.cacheLevel = validateAndReturnEnumValue(CdnCacheLevel.class, vmUpdateCdnRequest.cacheLevel);
        request.customerId = credit.getCustomerId();

        return createActionAndExecute(actionService, commandService, vmId,
                ActionType.MODIFY_CDN,  request, "Vps4ModifyCdnSite", user);
    }
}
