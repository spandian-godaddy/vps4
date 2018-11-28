package com.godaddy.vps4.web.vm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.orchestration.vm.provision.ProvisionRequest;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
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
import com.godaddy.vps4.vm.ImageService;
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
import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.godaddy.vps4.web.util.RequestValidation.getAndValidateUserAccountCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateCreditIsNotInUse;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateResellerCredit;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsStopped;
import static com.godaddy.vps4.web.util.RequestValidation.validateUserIsShopper;
import static com.godaddy.vps4.web.util.RequestValidation.validateVmExists;
import static com.godaddy.vps4.web.util.VmHelper.createActionAndExecute;

@Vps4Api
@Api(tags = { "vms" })
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
    private final ImageService imageService;
    private final ActionService actionService;
    private final CommandService commandService;
    private final VmSnapshotResource vmSnapshotResource;
    private final Config config;
    private final String sgidPrefix;
    private final Cryptography cryptography;
    private final SchedulerWebService schedulerWebService;
    private final DataCenterService dcService;
    private final VmActionResource vmActionResource;
    private final SnapshotService snapshotService;

    @Inject
    public VmResource(GDUser user, VmService vmService, Vps4UserService vps4UserService,
            VirtualMachineService virtualMachineService, CreditService creditService, ProjectService projectService,
            ImageService imageService, ActionService actionService, CommandService commandService,
            VmSnapshotResource vmSnapshotResource, Config config, Cryptography cryptography,
            SchedulerWebService schedulerWebService, DataCenterService dcService, VmActionResource vmActionResource,
            SnapshotService snapshotService) {
        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.vps4UserService = vps4UserService;
        this.creditService = creditService;
        this.vmService = vmService;
        this.projectService = projectService;
        this.imageService = imageService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.vmSnapshotResource = vmSnapshotResource;
        this.config = config;
        this.schedulerWebService = schedulerWebService;
        this.dcService = dcService;
        sgidPrefix = this.config.get("hfs.sgid.prefix", "vps4-undefined-");
        this.cryptography = cryptography;
        this.vmActionResource = vmActionResource;
        this.snapshotService = snapshotService;
    }

    @GET
    @Path("/{vmId}")
    public VirtualMachine getVm(@PathParam("vmId") UUID vmId) {
        logger.info("getting vm with id {}", vmId);
        VirtualMachine virtualMachine = virtualMachineService.getVirtualMachine(vmId);

        validateVmExists(vmId, virtualMachine, user);
        logger.debug(String.format("VM valid until: %s | %s", virtualMachine.validUntil, Instant.now()));

        if (user.isShopper())
            getAndValidateUserAccountCredit(creditService, virtualMachine.orionGuid, user.getShopperId());
        return virtualMachine;
    }

    @POST
    @Path("{vmId}/start")
    public VmAction startVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);
        validateNoConflictingActions(vmId, actionService, ActionType.START_VM, ActionType.STOP_VM,
                ActionType.RESTART_VM, ActionType.RESTORE_VM);
        validateServerIsStopped(vmService.getVm(vm.hfsVmId));

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
        validateServerIsActive(vmService.getVm(vm.hfsVmId));

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
                ActionType.RESTART_VM, ActionType.POWER_CYCLE, ActionType.RESTORE_VM, ActionType.UPGRADE_VM, ActionType.REBUILD_VM);
        validateServerIsActive(vmService.getVm(vm.hfsVmId));

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
    }

    @POST
    @Path("/")
    public VmAction provisionVm(ProvisionVmRequest provisionRequest) {
        logger.info("provisioning vm with orionGuid {}", provisionRequest.orionGuid);

        validateUserIsShopper(user);
        VirtualMachineCredit vmCredit = getAndValidateUserAccountCredit(creditService, provisionRequest.orionGuid,
                user.getShopperId());
        validateCreditIsNotInUse(vmCredit);
        validateResellerCredit(dcService, vmCredit.resellerId, provisionRequest.dataCenterId);

        if (imageService.getImages(vmCredit.operatingSystem, vmCredit.controlPanel, provisionRequest.image, vmCredit.tier)
                .size() == 0) {
            // verify that the image matches the request (control panel, managed level, OS)
            String message = String.format("The image %s is not valid for this credit.", provisionRequest.image);
            throw new Vps4Exception("INVALID_IMAGE", message);
        }

        ProvisionVirtualMachineParameters params;
        VirtualMachine virtualMachine;
        Vps4User vps4User = vps4UserService.getOrCreateUserForShopper(user.getShopperId(), vmCredit.resellerId);
        try {
            params = new ProvisionVirtualMachineParameters(vps4User.getId(), provisionRequest.dataCenterId, sgidPrefix,
                    provisionRequest.orionGuid, provisionRequest.name, vmCredit.tier, vmCredit.managedLevel,
                    provisionRequest.image);
            virtualMachine = virtualMachineService.provisionVirtualMachine(params);
            creditService.claimVirtualMachineCredit(provisionRequest.orionGuid, provisionRequest.dataCenterId,
                    virtualMachine.vmId);
        } catch (Exception e) {
            throw new Vps4Exception("PROVISION_VM_FAILED", e.getMessage(), e);
        }

        Project project = projectService.getProject(virtualMachine.projectId);

        long actionId = actionService.createAction(virtualMachine.vmId, ActionType.CREATE_VM,
                new JSONObject().toJSONString(), user.getUsername());
        logger.info("VmAction id: {}", actionId);

        int mailRelayQuota =  Integer.parseInt(ResellerConfigHelper.getResellerConfig(config, vmCredit.resellerId, "mailrelay.quota", "5000"));

        ProvisionVmInfo vmInfo = new ProvisionVmInfo(virtualMachine.vmId, vmCredit.managedLevel, vmCredit.hasMonitoring(),
                virtualMachine.image, project.getVhfsSgid(), mailRelayQuota, virtualMachine.spec.diskGib);
        logger.info("vmInfo: {}", vmInfo.toString());
        byte[] encryptedPassword = cryptography.encrypt(provisionRequest.password);

        ProvisionRequest request = createProvisionRequest(provisionRequest.image, provisionRequest.username,
                project, virtualMachine.spec, actionId, vmInfo, user.getShopperId(), provisionRequest.name,
                provisionRequest.orionGuid, encryptedPassword);

        String provisionClassName = virtualMachine.spec.isVirtualMachine() ? "ProvisionVm" : "ProvisionDedicated";
        CommandState command = Commands.execute(commandService, actionService, provisionClassName, request);
        logger.info("running {} in {}", provisionClassName, command.commandId);

        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }

    private ProvisionRequest createProvisionRequest(String image, String username, Project project,
                                                    ServerSpec spec, long actionId, ProvisionVmInfo vmInfo,
                                                    String shopperId, String serverName,
                                                    UUID orionGuid, byte[] encryptedPassword) {
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
        request.zone = spec.isVirtualMachine() ? config.get("openstack.zone") : config.get("ovh.zone");
        return request;
    }

    @DELETE
    @Path("/{vmId}")
    public VmAction destroyVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = getVm(vmId);

        cancelIncompleteVmActions(vmId);

        // delete all snapshots associated with the VM
        destroyVmSnapshots(vmId);

        String destroyMethod = vm.spec.serverType.serverType == ServerType.Type.DEDICATED ? "Vps4DestroyDedicated" : "Vps4DestroyVm";

        VmActionRequest destroyRequest = new VmActionRequest();
        destroyRequest.virtualMachine = vm;
        VmAction deleteAction = createActionAndExecute(actionService, commandService, vm.vmId,
                ActionType.DESTROY_VM, destroyRequest, destroyMethod, user);

        creditService.unclaimVirtualMachineCredit(vm.orionGuid);
        virtualMachineService.setVmRemoved(vm.vmId);

        return deleteAction;
    }

    private void destroyVmSnapshots(UUID vmId) {
        List<Snapshot> snapshots = vmSnapshotResource.getSnapshotsForVM(vmId);
        for (Snapshot snapshot : snapshots) {
            if(snapshot.status == SnapshotStatus.NEW || snapshot.status == SnapshotStatus.ERROR){
                // just mark snapshots as cancelled if they were new or errored
                snapshotService.updateSnapshotStatus(snapshot.id, SnapshotStatus.CANCELLED);
            }
            else {
                vmSnapshotResource.destroySnapshot(vmId, snapshot.id);
            }
        }
    }

    private void cancelIncompleteVmActions(UUID vmId) {
        List<Action> actions = actionService.getIncompleteActions(vmId);
        for (Action action: actions) {
            vmActionResource.cancelVmAction(vmId, action.id);
        }
    }

    @GET
    @Path("/")
    @ApiOperation(value = "Get VMs")
    public List<VirtualMachine> getVirtualMachines(
            @ApiParam(value = "The type of VMs to return", required = false) @DefaultValue("ACTIVE") @QueryParam("type") VirtualMachineType type,
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
        if (StringUtils.isBlank(user.getShopperId()))
            throw new Vps4NoShopperException();
        Vps4User vps4User = vps4UserService.getUser(user.getShopperId());
        if(vps4User == null) {
            return new ArrayList<VirtualMachine>();
        }

        return virtualMachineService.getVirtualMachines(type, vps4User.getId(), null, null, null);
    }

    @GET
    @Path("/{vmId}/details")
    public VirtualMachineDetails getVirtualMachineDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = getVm(vmId);
        Vm vm = getVmFromVmVertical(virtualMachine.hfsVmId);
        return new VirtualMachineDetails(vm);
    }

    @GET
    @Path("/{vmId}/hfsDetails")
    public Vm getMoreDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = getVm(vmId);
        return getVmFromVmVertical(virtualMachine.hfsVmId);
    }

    public Vm getVmFromVmVertical(long vmId) {
        try {
            return vmService.getVm(vmId);
        } catch (Exception e) {
            logger.warn("Cannot find VM ID {} in vm vertical", vmId);
            return null;
        }
    }

    @GET
    @Path("/{vmId}/withDetails")
    public VirtualMachineWithDetails getVirtualMachineWithDetails(@PathParam("vmId") UUID vmId) {
        VirtualMachine virtualMachine = getVm(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(virtualMachine.orionGuid);
        Vm vm = getVmFromVmVertical(virtualMachine.hfsVmId);
        SnapshotSchedule snapshotSchedule = new SnapshotSchedule();
        if (virtualMachine.backupJobId != null) {
            SchedulerJobDetail job = schedulerWebService.getJob("vps4", "backups", virtualMachine.backupJobId);
            if (job != null) {
                Instant nextRun = job.nextRun;
                int repeatIntervalInDays = job.jobRequest.repeatIntervalInDays;
                int copiesToRetain = 1;
                snapshotSchedule = new SnapshotSchedule(nextRun, copiesToRetain, repeatIntervalInDays);
            }
        }
        return new VirtualMachineWithDetails(virtualMachine, new VirtualMachineDetails(vm), credit.dataCenter, credit.shopperId, snapshotSchedule);
    }

}
