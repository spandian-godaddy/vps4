package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateCreditIsNotInUse;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.mailrelay.MailRelay;
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.ECommCreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.mailrelay.MailRelayService;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.provision.ProvisionRequest;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.ProvisionVmInfo;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VirtualMachineService.ProvisionVirtualMachineParameters;
import com.godaddy.vps4.vm.VirtualMachineType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.Vps4UserNotFound;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.util.ResellerConfigHelper;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
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
    private final VmSnapshotResource vmSnapshotResource;
    private final Config config;
    private final String sgidPrefix;
    private final Cryptography cryptography;
    private final DataCenterService dcService;
    private final VmActionResource vmActionResource;
    private final SnapshotService snapshotService;
    private final ImageResource imageResource;
    private final MailRelayService mailRelayService;

    @Inject
    public VmResource(GDUser user,
                      VmService vmService,
                      Vps4UserService vps4UserService,
                      VirtualMachineService virtualMachineService,
                      CreditService creditService,
                      ProjectService projectService,
                      ActionService actionService,
                      CommandService commandService,
                      VmSnapshotResource vmSnapshotResource,
                      Config config,
                      Cryptography cryptography,
                      DataCenterService dcService,
                      VmActionResource vmActionResource,
                      SnapshotService snapshotService,
                      ImageResource imageResource,
                      MailRelayService mailRelayService) {
        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.vps4UserService = vps4UserService;
        this.creditService = creditService;
        this.vmService = vmService;
        this.projectService = projectService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.vmSnapshotResource = vmSnapshotResource;
        this.config = config;
        this.dcService = dcService;
        sgidPrefix = this.config.get("hfs.sgid.prefix", "vps4-undefined-");
        this.cryptography = cryptography;
        this.vmActionResource = vmActionResource;
        this.snapshotService = snapshotService;
        this.imageResource = imageResource;
        this.mailRelayService = mailRelayService;
    }

    @GET
    @Path("/{vmId}")
    public VirtualMachine getVm(@PathParam("vmId") UUID vmId) {
        logger.info("getting vm with id {}", vmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        validateVmExists(vmId, virtualMachine, user);
        logger.debug(String.format("VM valid until: %s | %s", virtualMachine.validUntil, Instant.now()));

        if (user.isShopper()) {
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

    public static class ProvisionVmRequest {
        public String name;
        public UUID orionGuid;
        public String image;
        public int dataCenterId;
        public String username;
        public String password;
        public boolean useBetaMonitoring;
    }

    @POST
    @Path("/")
    public VmAction provisionVm(ProvisionVmRequest provisionRequest) {
        logger.info("provisioning vm with orionGuid {}", provisionRequest.orionGuid);

        validateUserIsShopper(user);
        VirtualMachineCredit vmCredit = getAndValidateUserAccountCredit(creditService, provisionRequest.orionGuid, user.getShopperId());
        validateCreditIsNotInUse(vmCredit);
        if (vmCredit.isDed4()) {
            validateDedResellerSelectedDc(dcService, vmCredit.getResellerId(), provisionRequest.dataCenterId);
        }

        Image image = imageResource.getImage(provisionRequest.image);
        validateRequestedImage(vmCredit, image);
        validatePassword(provisionRequest.password, image);

        int previousRelays = getPreviousRelaysForVirtualServers(provisionRequest, image);
        ProvisionVirtualMachineParameters params;
        VirtualMachine virtualMachine;
        Vps4User vps4User = vps4UserService.getOrCreateUserForShopper(user.getShopperId(), vmCredit.getResellerId());
        try {
            params = new ProvisionVirtualMachineParameters(vps4User.getId(), provisionRequest.dataCenterId, sgidPrefix,
                                                           provisionRequest.orionGuid, provisionRequest.name,
                                                           vmCredit.getTier(), vmCredit.getManagedLevel(),
                                                           provisionRequest.image);
            virtualMachine = virtualMachineService.provisionVirtualMachine(params);
            virtualMachineService.setMonitoringPlanFeature(virtualMachine.vmId, (vmCredit.getMonitoring() == 0)?false:true);
            creditService.claimVirtualMachineCredit(provisionRequest.orionGuid, provisionRequest.dataCenterId,
                                                    virtualMachine.vmId);
        } catch (Exception e) {
            throw new Vps4Exception("PROVISION_VM_FAILED", e.getMessage(), e);
        }

        Project project = projectService.getProject(virtualMachine.projectId);

        long actionId = actionService.createAction(virtualMachine.vmId, ActionType.CREATE_VM,
                                                   new JSONObject().toJSONString(), user.getUsername());
        logger.info("VmAction id: {}", actionId);

        int mailRelayQuota = Integer.parseInt(
                ResellerConfigHelper.getResellerConfig(config, vmCredit.getResellerId(), "mailrelay.quota", "5000"));

        ProvisionVmInfo vmInfo =
                new ProvisionVmInfo(virtualMachine.vmId,
                                    vmCredit.isManaged(),
                                    vmCredit.hasMonitoring(),
                                    virtualMachine.image,
                                    project.getVhfsSgid(),
                                    mailRelayQuota,
                                    virtualMachine.spec.diskGib,
                                    previousRelays);
        vmInfo.isPanoptaEnabled = provisionRequest.useBetaMonitoring;
        logger.info("vmInfo: {}", vmInfo);
        byte[] encryptedPassword = cryptography.encrypt(provisionRequest.password);

        ProvisionRequest request =
                createProvisionRequest(provisionRequest.image,
                                       provisionRequest.username,
                                       project,
                                       virtualMachine.spec,
                                       actionId,
                                       vmInfo,
                                       user.getShopperId(),
                                       provisionRequest.name,
                                       provisionRequest.orionGuid,
                                       encryptedPassword,
                                       vmCredit.getResellerId()
                                      );

        String provisionClassName = virtualMachine.spec.serverType.platform.getProvisionCommand();

        CommandState command = Commands.execute(commandService, actionService, provisionClassName, request);
        logger.info("running {} in {}", provisionClassName, command.commandId);

        return new VmAction(actionService.getAction(actionId), user.isEmployee());
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

    private ProvisionRequest createProvisionRequest(String image, String username, Project project,
                                                    ServerSpec spec, long actionId, ProvisionVmInfo vmInfo,
                                                    String shopperId, String serverName,
                                                    UUID orionGuid, byte[] encryptedPassword,
                                                    String resellerId) {
        ProvisionRequest request = new ProvisionRequest();
        request.actionId = actionId;
        request.image_name = image;
        request.username = username;
        request.sgid = project.getVhfsSgid();
        request.rawFlavor = spec.specName;
        request.vmInfo = vmInfo;
        request.shopperId = shopperId;
        request.serverName = serverName;
        request.orionGuid = orionGuid;
        request.encryptedPassword = encryptedPassword;
        request.zone = config.get(spec.serverType.platform.getZone());
        request.privateLabelId = resellerId;
        return request;
    }


    @DELETE
    @Path("/{vmId}")
    public VmAction destroyVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);

        cancelIncompleteVmActions(vmId);

        // delete all snapshots associated with the VM
        destroyVmSnapshots(vmId);

        String destroyMethod = vm.spec.serverType.platform.getDestroyCommand();

        VmActionRequest destroyRequest = new VmActionRequest();
        destroyRequest.virtualMachine = vm;
        VmAction deleteAction = createActionAndExecute(actionService, commandService, vm.vmId,
                                                       ActionType.DESTROY_VM, destroyRequest, destroyMethod, user);

        int mailRelays = getMailRelays(vm);

        creditService.unclaimVirtualMachineCredit(vm.orionGuid, vm.vmId, mailRelays);
        virtualMachineService.setVmRemoved(vm.vmId);

        return deleteAction;
    }

    private int getMailRelays(VirtualMachine vm) {
        if(vm.primaryIpAddress == null)
            return 0;

        int mailRelays = 0;
        if(vm.spec.serverType.serverType == ServerType.Type.VIRTUAL) {
            try {
                MailRelay relay = mailRelayService.getMailRelay(vm.primaryIpAddress.ipAddress);
                mailRelays = relay.relays;
            }
            catch (NotFoundException e) {
                logger.debug("Mail relay record not found for ip {}", vm.primaryIpAddress.ipAddress);
            }
            catch (Exception e) {
                logger.debug("Failed to find mail relays for vm {}, setting mail relay count to 0", vm.vmId);
            }
        }
        return mailRelays;
    }

    private void destroyVmSnapshots(UUID vmId) {
        List<Snapshot> snapshots = vmSnapshotResource.getSnapshotsForVM(vmId);
        for (Snapshot snapshot : snapshots) {
            if (snapshot.status == SnapshotStatus.NEW
                    || snapshot.status == SnapshotStatus.ERROR
                    || snapshot.status == SnapshotStatus.ERROR_RESCHEDULED
                    || snapshot.status == SnapshotStatus.LIMIT_RESCHEDULED
                    || snapshot.status == SnapshotStatus.AGENT_DOWN) {
                // just mark snapshots as cancelled if they were new or errored
                snapshotService.updateSnapshotStatus(snapshot.id, SnapshotStatus.CANCELLED);
            } else {
                vmSnapshotResource.destroySnapshot(vmId, snapshot.id);
            }
        }
    }

    private void cancelIncompleteVmActions(UUID vmId) {
        List<Action> actions = actionService.getIncompleteActions(vmId);
        for (Action action : actions) {
            vmActionResource.cancelVmAction(vmId, action.id);
        }
    }

    @GET
    @Path("/")
    @ApiOperation(value = "Get VMs")
    public List<VirtualMachine> getVirtualMachines(
            @ApiParam(value = "The type of VMs to return", required = false) @DefaultValue("ACTIVE") @QueryParam(
                    "type") VirtualMachineType type,
            @ApiParam(value = "Shopper ID of the user", required = false) @QueryParam("shopperId") String shopperId,
            @ApiParam(value = "IP Address of the desired VM", required = false) @QueryParam("ipAddress") String ipAddress,
            @ApiParam(value = "Orion Guid associated with the VM", required = false) @QueryParam("orionGuid") UUID orionGuid,
            @ApiParam(value = "HFS VM ID associated with the VM", required = false) @QueryParam("hfsVmId") Long hfsVmId) {
        if (user.isEmployee()) {
            return getVmsForEmployee(type, shopperId, ipAddress, orionGuid, hfsVmId);
        }
        return getVmsForVps4User(type);
    }

    private List<VirtualMachine> getVmsForEmployee(VirtualMachineType type, String shopperId, String ipAddress,
                                                   UUID orionGuid, Long hfsVmId) {
        List<VirtualMachine> vmList = new ArrayList<>();
        try {
            Long vps4UserId = getUserId(shopperId);
            vmList = virtualMachineService.getVirtualMachines(type, vps4UserId, ipAddress, orionGuid, hfsVmId);
        } catch (Vps4UserNotFound ex) {
            logger.warn("Shopper not found", ex);
        }

        return vmList;
    }

    private Long getUserId(String shopperId) throws Vps4UserNotFound {
        //Shopper ID from impersonation takes priority over the Shopper ID query parameter.
        if (user.isEmployeeToShopper()) {
            return getVps4UserIdByShopperId(user.getShopperId());
        } else if (!StringUtils.isBlank(shopperId)) {
            return getVps4UserIdByShopperId(shopperId);
        }
        return null;
    }

    private Long getVps4UserIdByShopperId(String shopperId) throws Vps4UserNotFound {
        Vps4User vps4User = vps4UserService.getUser(shopperId);
        if (vps4User == null) {
            throw new Vps4UserNotFound("User not found for Shopper ID: " + shopperId);
        }
        return vps4User.getId();
    }

    private List<VirtualMachine> getVmsForVps4User(VirtualMachineType type) {
        if (StringUtils.isBlank(user.getShopperId())) {
            throw new Vps4NoShopperException();
        }
        Vps4User vps4User = vps4UserService.getUser(user.getShopperId());
        if (vps4User == null) {
            return new ArrayList<VirtualMachine>();
        }

        return virtualMachineService.getVirtualMachines(type, vps4User.getId(), null, null, null);
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
