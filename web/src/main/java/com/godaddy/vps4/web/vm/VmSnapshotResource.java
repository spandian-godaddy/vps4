package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.snapshot.SnapshotStatus.CANCELLED;
import static com.godaddy.vps4.snapshot.SnapshotStatus.DESTROYED;
import static com.godaddy.vps4.web.util.RequestValidation.validateAgentIsOk;
import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotOverQuota;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoOtherSnapshotsInProgress;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotBelongsToVm;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotExists;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotName;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotNotPaused;
import static com.godaddy.vps4.web.util.RequestValidation.validateUserIsShopper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonView;
import com.godaddy.hfs.config.Config;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.security.Views;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.util.TroubleshootVmService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;
import com.google.inject.Provider;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class VmSnapshotResource {
    private static final Logger logger = LoggerFactory.getLogger(VmSnapshotResource.class);

    private final GDUser user;
    private final VmResource vmResource;
    private final Provider<VmSnapshotActionResource> actionResourceProvider;
    private final ActionService actionService;
    private final CommandService commandService;
    private final OhBackupService ohBackupService;
    private final SchedulerWebService schedulerWebService;
    private final SnapshotService snapshotService;
    private final TroubleshootVmService troubleshootVmService;
    private final VmService vmService;
    private final Config config;

    @Inject
    public VmSnapshotResource(GDUser user,
                              VmResource vmResource,
                              Provider<VmSnapshotActionResource> actionResourceProvider,
                              @SnapshotActionService ActionService actionService,
                              CommandService commandService,
                              OhBackupService ohBackupService,
                              SchedulerWebService schedulerWebService,
                              SnapshotService snapshotService,
                              TroubleshootVmService troubleshootVmService,
                              VmService vmService,
                              Config config) {
        this.user = user;
        this.vmResource = vmResource;
        this.actionResourceProvider = actionResourceProvider;
        this.actionService = actionService;
        this.commandService = commandService;
        this.ohBackupService = ohBackupService;
        this.schedulerWebService = schedulerWebService;
        this.snapshotService = snapshotService;
        this.troubleshootVmService = troubleshootVmService;
        this.vmService = vmService;
        this.config = config;
    }

    @GET
    @Path("/{vmId}/snapshots")
    @JsonView(Views.Public.class)
    public List<Snapshot> getSnapshotsForVM(@PathParam("vmId") UUID vmId) {
        vmResource.getVm(vmId); // auth validation
        return snapshotService.getSnapshotsForVm(vmId)
                              .stream()
                              .filter(snapshot -> snapshot.status != DESTROYED && snapshot.status != CANCELLED)
                              .collect(Collectors.toList());
    }

    public static class VmSnapshotRequest {
        public String name;
        public SnapshotType snapshotType = SnapshotType.ON_DEMAND;
    }

    @POST
    @Path("/{vmId}/snapshots")
    public SnapshotAction createSnapshot(@PathParam("vmId") UUID vmId, VmSnapshotRequest request) {
        if (Boolean.parseBoolean(config.get("vps4.snapshot.currentlyPaused"))) {
            throw new Vps4Exception("JOB_PAUSED", "Currently pausing all snapshot jobs. Refusing to take snapshot.");
        }

        VirtualMachine vm = vmResource.getVm(vmId); // auth validation
        validateVmCanCreateSnapshot(vm, request);

        Action action = createSnapshotAndActionEntries(vm, request.name, request.snapshotType);
        kickoffSnapshotCreation(vm.vmId, vm.hfsVmId, action, vm.orionGuid, request.snapshotType, user.getShopperId());
        return new SnapshotAction(actionService.getAction(action.id), user.isEmployee());
    }

    private Action createSnapshotAndActionEntries(VirtualMachine vm, String snapshotName, SnapshotType snapshotType) {
        UUID snapshotId = snapshotService.createSnapshot(vm.projectId, vm.vmId, snapshotName, snapshotType);
        long actionId = actionService.createAction(
                snapshotId, ActionType.CREATE_SNAPSHOT, new JSONObject().toJSONString(), user.getUsername());
        logger.info("Creating db entries for snapshot creation. Snapshot Action ID: {}, Snapshot ID: {}", actionId, snapshotId);
        return actionService.getAction(actionId);
    }

    private void validateVmCanCreateSnapshot(VirtualMachine vm, VmSnapshotRequest request) {
        validateUserIsShopper(user);
        validateSnapshotName(request.name);
        validateIfSnapshotOverQuota(ohBackupService, snapshotService, vm, request.snapshotType);
        validateNoOtherSnapshotsInProgress(ohBackupService, snapshotService, vm);
        validateSnapshotNotPaused(schedulerWebService, vm.backupJobId, request.snapshotType);

        if (vm.spec.serverType.platform == ServerType.Platform.OPENSTACK) {
            validateAgentIsOk(vm, vmService, troubleshootVmService);
        }

        validateDcLimit(vm.vmId, request);
        validateHvLimitAndSaveToDb(vm.vmId, vm.hfsVmId, request);
    }

    private void validateDcLimit(UUID vmId, VmSnapshotRequest request) {
        if (!request.snapshotType.equals((SnapshotType.AUTOMATIC)))
            return;
        int currentLoad = snapshotService.totalSnapshotsInProgress();
        int maxAllowed = Integer.parseInt(config.get("vps4.autobackup.concurrentLimit", "50"));
        if (currentLoad >= maxAllowed) {
            logger.info("Cannot create snapshot for vmID {} because of too many concurrent snapshots", vmId);
            throw new Vps4Exception("SNAPSHOT_DC_LIMIT_REACHED", String.format("Too many concurrent snapshots. Cannot create snapshot for vmID %s now", vmId));
        }
    }

    private void validateHvLimitAndSaveToDb(UUID vmId, long hfsVmId, VmSnapshotRequest request) {
        if (!Boolean.parseBoolean(config.get("vps4.autobackup.checkHvConcurrentLimit")))
            return;
        if (!request.snapshotType.equals((SnapshotType.AUTOMATIC)))
            return;
        String hypervisorHostname = getVmHypervisorHostname(vmId, hfsVmId);
        if (hypervisorHostname != null) {
            UUID conflictingVmId = snapshotService.getVmIdWithInProgressSnapshotOnHv(hypervisorHostname);
            if (conflictingVmId != null) { // limit 1 snapshot per HV
                logger.info("Cannot create snapshot for VM {} because VM {} already has snapshot in progress on {}",
                            vmId, conflictingVmId, hypervisorHostname);
                throw new Vps4Exception("SNAPSHOT_HV_LIMIT_REACHED", "Only 1 snapshot allowed per hypervisor");
            }
            snapshotService.saveVmHvForSnapshotTracking(vmId, hypervisorHostname);
        }
    }

    private String getVmHypervisorHostname(UUID vmId, long hfsVmId) {
        VmExtendedInfo vmExtendedInfo = vmService.getVmExtendedInfo(hfsVmId);
        if(vmExtendedInfo != null) {
            String hypervisorHostname = vmExtendedInfo.extended.hypervisorHostname;
            logger.info("Getting vm {} hypervisor: {}", vmId, hypervisorHostname);
            return hypervisorHostname;
        }
        return null;
    }

    private void kickoffSnapshotCreation(UUID vmId, long hfsVmId, Action action,
                                         UUID orionGuid, SnapshotType snapshotType, String shopperId) {
        UUID snapshotId = action.resourceId; // the resourceId refers to the associated snapshotId
        Vps4SnapshotVm.Request commandRequest = new Vps4SnapshotVm.Request();
        commandRequest.vmId = vmId;
        commandRequest.hfsVmId = hfsVmId;
        commandRequest.vps4SnapshotId = snapshotId;
        commandRequest.actionId = action.id;
        commandRequest.orionGuid = orionGuid;
        commandRequest.snapshotType = snapshotType;
        commandRequest.shopperId = shopperId;
        commandRequest.initiatedBy = user.getUsername();

        CommandState command = Commands.execute(
                commandService, actionService, "Vps4SnapshotVm", commandRequest);
        logger.info(
                "Creating snapshot {} for vps4 vm {} with command {}:{}",
                snapshotId, vmId, action.id, command.commandId
                   );
    }

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}")
    @JsonView(Views.Public.class)
    public Snapshot getSnapshot(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        vmResource.getVm(vmId); // auth validation
        Snapshot snapshot = snapshotService.getSnapshot(snapshotId);

        validateSnapshotExists(snapshotId, snapshot, user);
        validateSnapshotBelongsToVm(vmId, snapshot);

        return snapshot;
    }

    @GET
    @Path("/{vmId}/snapshots/{snapshotId}/withDetails")
    @JsonView(Views.Internal.class)
    public Snapshot getSnapshotWithDetails(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        return getSnapshot(vmId, snapshotId);
    }

    @DELETE
    @Path("/{vmId}/snapshots/{snapshotId}")
    public SnapshotAction destroySnapshot(@PathParam("vmId") UUID vmId, @PathParam("snapshotId") UUID snapshotId) {
        Snapshot snapshot = getSnapshot(vmId, snapshotId); // auth validation
        snapshotService.deleteVmHvForSnapshotTracking(snapshot.vmId);

        // Note: this doesn't do anything on the OpenStack side, just updates our database
        cancelIncompleteSnapshotActions(vmId, snapshotId);

        long actionId = actionService.createAction(snapshotId,  ActionType.DESTROY_SNAPSHOT,
                                                   new JSONObject().toJSONString(), user.getUsername());

        Vps4DestroySnapshot.Request request = new Vps4DestroySnapshot.Request();
        request.hfsSnapshotId = snapshot.hfsSnapshotId;
        request.vps4SnapshotId = snapshot.id;
        request.actionId = actionId;
        request.vmId = snapshot.vmId;

        CommandState command = Commands.execute(commandService, actionService, "Vps4DestroySnapshot", request);
        logger.info("Destroying snapshot {}:{} for vps4 vm {} with command {}:{}",
                    snapshotId, snapshot.name, snapshot.vmId, actionId, command.commandId);

        return new SnapshotAction(actionService.getAction(actionId), user.isEmployee());
    }

    private void cancelIncompleteSnapshotActions(UUID vmId, UUID snapshotId) {
        List<Action> actions = actionService.getIncompleteActions(snapshotId);
        for (Action action: actions) {
            actionResourceProvider.get().cancelSnapshotAction(vmId, snapshotId, action.id);
        }
    }

    @PATCH
    @Path("/{vmId}/snapshots/{snapshotId}")
    public SnapshotAction renameSnapshot(@PathParam("vmId") UUID vmId,
                                         @PathParam("snapshotId") UUID snapshotId,
                                         SnapshotRenameRequest request) {
        Snapshot snapshot = getSnapshot(vmId, snapshotId); // auth validation
        validateSnapshotName(request.name);
        String notes = String.format("Old name = %s, New Name = %s", snapshot.name, request.name);

        long actionId = actionService.createAction(snapshotId,  ActionType.RENAME_SNAPSHOT, new JSONObject().toJSONString(), user.getUsername());

        snapshotService.renameSnapshot(snapshotId, request.name);
        actionService.completeAction(actionId, new JSONObject().toJSONString(), notes);

        return new SnapshotAction(actionService.getAction(actionId), user.isEmployee());
    }

    public static class SnapshotRenameRequest {
        public String name;
    }
}
