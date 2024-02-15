package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateCreditE2S;
import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;
import static com.godaddy.vps4.web.util.RequestValidation.validateCreditIsNotInUse;
import static com.godaddy.vps4.web.util.RequestValidation.validateDcIdIsAllowed;
import static com.godaddy.vps4.web.util.RequestValidation.validateDedResellerSelectedDc;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validatePassword;
import static com.godaddy.vps4.web.util.RequestValidation.validateRequestedImage;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActiveOrUnknown;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsStoppedOrUnknown;
import static com.godaddy.vps4.web.util.RequestValidation.validateUserIsShopper;
import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.hfs.vm.VmList;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.Vps4DestroyVm;
import com.godaddy.vps4.orchestration.vm.provision.ProvisionRequest;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.PleskLicenseType;
import com.godaddy.vps4.vm.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.Vps4UserNotFound;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.ResellerConfigHelper;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmResource {

    private static final Logger logger = LoggerFactory.getLogger(VmResource.class);

    private final GDUser user;
    private final VirtualMachineService virtualMachineService;
    private final Vps4UserService vps4UserService;
    private final CreditService creditService;
    private final VmService vmService;
    private final ProjectService projectService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final Config config;
    private final String sgidPrefix;
    private final Cryptography cryptography;
    private final DataCenterService dcService;
    private final ImageResource imageResource;
    private final DataCenterService dataCenterService;

    @Inject
    public VmResource(GDUser user,
                      VmService vmService,
                      Vps4UserService vps4UserService,
                      VirtualMachineService virtualMachineService,
                      CreditService creditService,
                      ProjectService projectService,
                      ActionService actionService,
                      CommandService commandService,
                      Config config,
                      Cryptography cryptography,
                      DataCenterService dcService,
                      ImageResource imageResource,
                      DataCenterService dataCenterService) {
        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.vps4UserService = vps4UserService;
        this.creditService = creditService;
        this.vmService = vmService;
        this.projectService = projectService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.config = config;
        this.dcService = dcService;
        sgidPrefix = this.config.get("hfs.sgid.prefix", "vps4-undefined-");
        this.cryptography = cryptography;
        this.imageResource = imageResource;
        this.dataCenterService = dataCenterService;
    }

    private String getCustomerJwt() {
        if (user.isShopper() && !user.isShopperInjected()) {
            return user.getToken().getJwt().getParsedString();
        }
        return null;
    }

    @GET
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.CUSTOMER, GDUser.Role.SUSPEND_AUTH, GDUser.Role.HS_AGENT,
            GDUser.Role.HS_LEAD, GDUser.Role.VPS4_API_READONLY, GDUser.Role.C3_OTHER, GDUser.Role.EMPLOYEE_OTHER})
    @Path("/{vmId}")
    public VirtualMachine getVm(@PathParam("vmId") UUID vmId) {
        logger.info("getting vm with id {}", vmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        validateVmExists(vmId, virtualMachine, user);
        logger.debug(String.format("VM valid until: %s | %s", virtualMachine.validUntil, Instant.now()));

        if(user.isEmployeeToShopper()) {
            getAndValidateCreditE2S(creditService, virtualMachine.orionGuid, user.getShopperId());
        } else if (user.isShopper()) {
            getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());
        }
        return virtualMachine;
    }

    @POST
    @Path("{vmId}/start")
    public VmAction startVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);
        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                                     ActionType.RESTART_VM, ActionType.RESTORE_VM);
        validateServerIsStoppedOrUnknown(vmService.getVm(vm.hfsVmId));

        VmActionRequest startRequest = new VmActionRequest();
        startRequest.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId,
                                      ActionType.START_VM, startRequest, "Vps4StartVm", user);
    }

    @POST
    @Path("{vmId}/stop")
    public VmAction stopVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);
        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                                     ActionType.RESTART_VM, ActionType.RESTORE_VM);
        validateServerIsActiveOrUnknown(vmService.getVm(vm.hfsVmId));

        VmActionRequest stopRequest = new VmActionRequest();
        stopRequest.virtualMachine = vm;
        return createActionAndExecute(actionService, commandService, vm.vmId, ActionType.STOP_VM,
                                      stopRequest, "Vps4StopVm", user);
    }

    @POST
    @Path("{vmId}/restart")
    public VmAction restartVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);

        VmActionRequest restartRequest = new VmActionRequest();
        restartRequest.virtualMachine = vm;

        // avoid sending restart request if other conflicting vm actions are in progress.
        // for example: a restart/stop/start vm or restore/upgrade/rebuild vm action is already in progress
        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                                     ActionType.RESTART_VM, ActionType.POWER_CYCLE, ActionType.RESTORE_VM,
                                     ActionType.UPGRADE_VM, ActionType.REBUILD_VM);
        validateServerIsActiveOrUnknown(vmService.getVm(vm.hfsVmId));

        if (vm.spec.isVirtualMachine()) {
            // restart virtual machine if we pass all validations
            return createActionAndExecute(actionService, commandService, vm.vmId,
                                          ActionType.RESTART_VM, restartRequest, "Vps4RestartVm", user);
        } else {
            // initiate dedicated vm reboot action
            return createActionAndExecute(actionService, commandService, vm.vmId,
                                          ActionType.POWER_CYCLE, restartRequest, "Vps4RebootDedicated", user);
        }
    }

    public static class ProvisionVmIntent {
        public List<Integer> intentIds;
        public String otherIntentDescription;
    }

    public static class ProvisionVmRequest {
        public String name;
        public UUID orionGuid;
        public String image;
        public int dataCenterId;
        public String username;
        public String password;
        public boolean useBetaMonitoring;
        public ProvisionVmIntent intents;
    }

    @POST
    @Path("/")
    public VmAction provisionVm(ProvisionVmRequest provisionVmRequest) {
        logger.info("provisioning vm with orionGuid {}", provisionVmRequest.orionGuid);

        int[] validDcIds = Arrays.stream(config.get("vps4.datacenter.ids").split(","))
                                 .mapToInt(Integer::parseInt)
                                 .toArray();
        validateDcIdIsAllowed(validDcIds, provisionVmRequest.dataCenterId);
        validateUserIsShopper(user);
        VirtualMachineCredit vmCredit = getAndValidateUserAccountCredit(creditService, provisionVmRequest.orionGuid, user.getShopperId());
        if (vmCredit.isDed4()) {
            validateDedResellerSelectedDc(dcService, vmCredit.getResellerId(), provisionVmRequest.dataCenterId);
        }

        Image image = imageResource.getImage(provisionVmRequest.image);
        validateRequestedImage(vmCredit, image);
        validatePassword(provisionVmRequest.password);

        int previousRelays = getPreviousRelaysForVirtualServers(provisionVmRequest, image);
        ProvisionVirtualMachineParameters params;
        VirtualMachine virtualMachine;
        Vps4User vps4User = vps4UserService.getOrCreateUserForShopper(user.getShopperId(), vmCredit.getResellerId(), vmCredit.getCustomerId());

        validateCreditIsNotInUse(vmCredit);
        try {
            params = new ProvisionVirtualMachineParameters(vps4User.getId(), provisionVmRequest.dataCenterId, sgidPrefix,
                                                           provisionVmRequest.orionGuid, provisionVmRequest.name,
                                                           vmCredit.getTier(), vmCredit.getManagedLevel(),
                                                           provisionVmRequest.image);
            virtualMachine = virtualMachineService.provisionVirtualMachine(params);
            creditService.claimVirtualMachineCredit(provisionVmRequest.orionGuid, provisionVmRequest.dataCenterId,
                                                    virtualMachine.vmId);
            virtualMachineService.setMonitoringPlanFeature(virtualMachine.vmId, vmCredit.getMonitoring() != 0);
        } catch (Exception e) {
            throw new Vps4Exception("PROVISION_VM_FAILED", e.getMessage(), e);
        }

        Project project = projectService.getProject(virtualMachine.projectId);

        int mailRelayQuota = Integer.parseInt(
                ResellerConfigHelper.getResellerConfig(config, vmCredit.getResellerId(), "mailrelay.quota", "5000"));

        ProvisionVmInfo vmInfo = new ProvisionVmInfo(virtualMachine.vmId,
                                                     vmCredit.isManaged(),
                                                     vmCredit.hasMonitoring(),
                                                     virtualMachine.image,
                                                     project.getVhfsSgid(),
                                                     mailRelayQuota,
                                                     virtualMachine.spec.diskGib,
                                                     previousRelays);
        if(image.hasPlesk()) {
            vmInfo.pleskLicenseType = validateAndReturnEnumValue(PleskLicenseType.class, vmCredit.getControlPanel());
        }

        vmInfo.isPanoptaEnabled = provisionVmRequest.useBetaMonitoring;
        logger.info("vmInfo: {}", vmInfo);
        byte[] encryptedPassword = cryptography.encrypt(provisionVmRequest.password);
        String zone = getZone(provisionVmRequest.dataCenterId, virtualMachine.spec.serverType.platform.getZone());

        ProvisionRequest provisionRequest =
                createProvisionRequest(provisionVmRequest.image,
                                       provisionVmRequest.username,
                                       project,
                                       virtualMachine.spec,
                                       vmInfo,
                                       user.getShopperId(),
                                       provisionVmRequest.name,
                                       provisionVmRequest.orionGuid,
                                       encryptedPassword,
                                       vmCredit.getResellerId(),
                                       zone, 
                                       provisionVmRequest.intents);

        String provisionClassName = virtualMachine.spec.serverType.platform.getProvisionCommand();

        return createActionAndExecute(actionService, commandService, virtualMachine.vmId, ActionType.CREATE_VM, provisionRequest,
                                      provisionClassName, user);
    }

    private String getZone(int dataCenterId, String platformZone) {
        DataCenter dc = dataCenterService.getDataCenter(dataCenterId);
        String zoneConfigLabel = dc.dataCenterName + "." + platformZone;
        return config.get(zoneConfigLabel);
    }

    private int getPreviousRelaysForVirtualServers(ProvisionVmRequest provisionRequest, Image image) {
        int previousRelays = 0;
        if(image.serverType.serverType == ServerType.Type.VIRTUAL) {
            Map<ECommCreditService.ProductMetaField, String> productMeta = creditService.getProductMeta(
                    provisionRequest.orionGuid);
            String previousRelaysString = productMeta.get(ECommCreditService.ProductMetaField.RELAY_COUNT);
            previousRelays = previousRelaysString==null ? 0 : Integer.parseInt(previousRelaysString);
            if(previousRelays > 0)
            {
                Instant releasedAt = Instant.parse(productMeta.get(ECommCreditService.ProductMetaField.RELEASED_AT)).truncatedTo(ChronoUnit.DAYS);
                Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
                if(releasedAt.isBefore(now))
                {
                    previousRelays = 0;
                }
            }
        }
        return previousRelays;
    }

    private ProvisionRequest createProvisionRequest(String image,
                                                    String username,
                                                    Project project,
                                                    ServerSpec spec,
                                                    ProvisionVmInfo vmInfo,
                                                    String shopperId,
                                                    String serverName,
                                                    UUID orionGuid,
                                                    byte[] encryptedPassword,
                                                    String resellerId,
                                                    String zone,
                                                    ProvisionVmIntent intents) {
        ProvisionRequest request = new ProvisionRequest();
        request.image_name = image;
        request.username = username;
        request.sgid = project.getVhfsSgid();
        request.rawFlavor = spec.specName;
        request.vmInfo = vmInfo;
        request.shopperId = shopperId;
        request.serverName = serverName;
        request.orionGuid = orionGuid;
        request.encryptedPassword = encryptedPassword;
        request.zone = zone;
        request.privateLabelId = resellerId;
        if(intents != null){
            request.intentIds = intents.intentIds;
            request.intentOtherDescription = intents.otherIntentDescription;
        }
        return request;
    }


    @DELETE
    @Path("/{vmId}")
    public VmAction destroyVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        String destroyMethod = vm.spec.serverType.platform.getDestroyCommand();

        Vps4DestroyVm.Request destroyRequest = new Vps4DestroyVm.Request();
        destroyRequest.virtualMachine = vm;
        destroyRequest.gdUserName = user.getUsername();
        destroyRequest.customerId = credit.getCustomerId();

        return createActionAndExecute(actionService, commandService, vm.vmId,
                                      ActionType.DESTROY_VM, destroyRequest, destroyMethod, user);
    }

    @GET
    @RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.CUSTOMER, GDUser.Role.SUSPEND_AUTH, GDUser.Role.HS_AGENT,
            GDUser.Role.HS_LEAD, GDUser.Role.VPS4_API_READONLY, GDUser.Role.C3_OTHER, GDUser.Role.EMPLOYEE_OTHER})
    @Path("/")
    @ApiOperation(value = "Get VMs")
    public List<VirtualMachine> getVirtualMachines(
            @ApiParam(value = "The type of VMs to return", required = false) @DefaultValue("ACTIVE") @QueryParam(
                    "type") VirtualMachineType type,
            @ApiParam(value = "Shopper ID of the user", required = false) @QueryParam("shopperId") String shopperId,
            @ApiParam(value = "IP Address of the desired VM", required = false) @QueryParam("ipAddress") String ipAddress,
            @ApiParam(value = "Orion Guid associated with the VM", required = false) @QueryParam("orionGuid") UUID orionGuid,
            @ApiParam(value = "HFS VM ID associated with the VM", required = false) @QueryParam("hfsVmId") Long hfsVmId,
            @ApiParam(value = "DC ID associated with the VM", required = false) @QueryParam("dcId") Integer dcId,
            @ApiParam(value = "Customer ID of the user", required = false) @QueryParam("customerId") UUID customerId,
            @ApiParam(value = "Platform of the VM", required = false) @QueryParam("platform") ServerType.Platform platform,
            @ApiParam(value = "Hypervisor associated with the VM. Ex: sg3plztncldhv000-00.prod.sin3.gdg", required = false) @QueryParam("hypervisor") String hypervisor) {

        if (user.isEmployee()) {
            return getVmsForEmployee(type, shopperId, customerId, ipAddress, orionGuid, hfsVmId, dcId, platform, hypervisor);
        }
        return getVmsForVps4User(type, dcId);
    }

    private List<Long> getHfsVmIdsOnHypervisor(ServerType.Platform platform, String hypervisor) {
        List<Long> vmIds = new ArrayList<>();
        VmList hfsVmList = getHfsVmsOnPlatformAndHV(platform, hypervisor);
        if (hfsVmList.results != null && !hfsVmList.results.isEmpty()) {
            vmIds = hfsVmList.results.stream().map(t -> t.vmId).collect(Collectors.toList());
        }
        return vmIds;
    }

    private VmList getHfsVmsOnPlatformAndHV(ServerType.Platform platform, String hypervisor) {
        if (platform == ServerType.Platform.OPENSTACK) {
            return vmService.listVmsOnHypervisor("openstack", hypervisor);
        }
        if (platform == ServerType.Platform.OPTIMIZED_HOSTING) {
            return vmService.listVmsOnHypervisor("virtuozzo_vm", hypervisor);
        }
        throw new Vps4Exception("INVALID_PLATFORM", "Platform must be either OPENSTACK or OPTIMIZED_HOSTING");
    }

    private List<VirtualMachine> getVmsForEmployee(VirtualMachineType type, String shopperId, UUID customerId, String ipAddress,
                                                   UUID orionGuid, Long hfsVmId, Integer dcId, ServerType.Platform platform,
                                                   String hypervisor) {
        List<Long> hfsVmIds = null;
        List<VirtualMachine> vmList = new ArrayList<>();
        if (hypervisor != null && hfsVmId != null) {
            throw new Vps4Exception("INVALID_FILTERS", "Both Hypervisor and Hfs Vm Id fields cannot be filled in.");
        } else if (hypervisor != null) {
            hfsVmIds = getHfsVmIdsOnHypervisor(platform, hypervisor);
        } else if (hfsVmId != null) {
            hfsVmIds = Collections.singletonList(hfsVmId);
        }
        try {
            Long vps4UserId = getUserId(shopperId, customerId);
            String platformStr = (hypervisor == null && platform != null) ? platform.toString() : null;
            vmList = virtualMachineService.getVirtualMachines(type, vps4UserId, ipAddress, orionGuid, dcId, platformStr, hfsVmIds);
        } catch (Vps4UserNotFound ex) {
            logger.warn("Shopper not found", ex);
        }

        return vmList;
    }

    private Long getUserId(String shopperId, UUID customerId) throws Vps4UserNotFound {
        //Shopper ID from impersonation takes priority over the Shopper ID query parameter.
        //Searches for Customer ID only if Shopper ID is null
        if (user.isEmployeeToShopper()) {
            return getVps4UserIdByShopperId(user.getShopperId());
        } else if (!StringUtils.isBlank(shopperId)) {
            return getVps4UserIdByShopperId(shopperId);
        } else if (customerId != null) {
            return getVps4UserIdByCustomerId(customerId);
        }

        return null;
    }

    private Long getVps4UserIdByCustomerId(UUID customerId) throws Vps4UserNotFound {
        Vps4User vps4User = vps4UserService.getUser(customerId);

        if (vps4User == null) {
            throw new Vps4UserNotFound("User not found for Customer ID: " + customerId);
        }
        return vps4User.getId();
    }

    private Long getVps4UserIdByShopperId(String shopperId) throws Vps4UserNotFound {
        Vps4User vps4User = vps4UserService.getUser(shopperId);

        if (vps4User == null) {
            throw new Vps4UserNotFound("User not found for Shopper ID: " + shopperId);
        }
        return vps4User.getId();
    }

    private List<VirtualMachine> getVmsForVps4User(VirtualMachineType type, Integer dcId) {
        if (StringUtils.isBlank(user.getShopperId())) {
            throw new Vps4NoShopperException();
        }
        Vps4User vps4User = vps4UserService.getUser(user.getShopperId());
        if (vps4User == null) {
            return new ArrayList<VirtualMachine>();
        }

        return virtualMachineService.getVirtualMachines(type, vps4User.getId(), null, null, dcId, null, null);
    }

    public Vm getVmFromVmVertical(long vmId) {
        try {
            return vmService.getVm(vmId);
        } catch (Exception e) {
            logger.warn("Cannot find VM ID {} in vm vertical", vmId);
            return null;
        }
    }

    public VmExtendedInfo getVmExtendedInfoFromVmVertical(long vmId) {
        try {
            return vmService.getVmExtendedInfo(vmId);
        } catch (Exception e) {
            logger.warn("Cannot get VM extended info in vm vertical for VM ID {}: {}", vmId, e.getMessage());
            return null;
        }
    }

}
