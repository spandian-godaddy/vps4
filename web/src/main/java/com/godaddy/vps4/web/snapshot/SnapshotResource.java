package com.godaddy.vps4.web.snapshot;

import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotOverQuota;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoOtherSnapshotsInProgress;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotExists;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotName;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotNotPaused;
import static com.godaddy.vps4.web.util.RequestValidation.validateUserIsShopper;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
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
import com.godaddy.hfs.vm.Vm;
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
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"snapshots"})

@Path("/api/snapshots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class SnapshotResource {
    private static final Logger logger = LoggerFactory.getLogger(SnapshotResource.class);

    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;
    private final SnapshotService snapshotService;
    private final SnapshotActionResource snapshotActionResource;
    private final SchedulerWebService schedulerWebService;
    private final VmService vmService;
    private final TroubleshootVmService troubleshootVmService;
    private final VmResource vmResource;
    private final OhBackupService ohBackupService;
    private final Config config;

    @Inject
    public SnapshotResource(@SnapshotActionService ActionService actionService,
                            CommandService commandService,
                            GDUser user,
                            SnapshotService snapshotService,
                            SnapshotActionResource snapshotActionResource,
                            SchedulerWebService schedulerWebService,
                            VmService vmService,
                            TroubleshootVmService troubleshootVmService,
                            VmResource vmResource,
                            OhBackupService ohBackupService,
                            Config config) {
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.snapshotService = snapshotService;
        this.snapshotActionResource = snapshotActionResource;
        this.schedulerWebService = schedulerWebService;
        this.vmService = vmService;
        this.troubleshootVmService = troubleshootVmService;
        this.vmResource = vmResource;
        this.ohBackupService = ohBackupService;
        this.config = config;
    }

    @POST
    @Path("/")
    public SnapshotAction createSnapshot(SnapshotRequest snapshotRequest) {
        VirtualMachine vm = vmResource.getVm(snapshotRequest.vmId); // auth validation
        validateVmCanCreateSnapshot(vm.orionGuid, vm.backupJobId, snapshotRequest.name, snapshotRequest.snapshotType);

        validateVmCanCreateCephSnapshot(snapshotRequest, vm);
        if (vm.spec.serverType.platform == ServerType.Platform.OPENSTACK) {
            throwErrorIfAgentIsDown(vm);
        }

        Action action = createSnapshotAndActionEntries(vm, snapshotRequest.name, snapshotRequest.snapshotType);
        kickoffSnapshotCreation(vm.vmId, vm.hfsVmId, action, vm.orionGuid, snapshotRequest.snapshotType, user.getShopperId());
        return new SnapshotAction(actionService.getAction(action.id), user.isEmployee());
    }

    public static class SnapshotRequest {
        public String name;
        public UUID vmId;
        public SnapshotType snapshotType;
    }

    private void throwErrorIfAgentIsDown(VirtualMachine vm) {
        Vm hfsVm = vmService.getVm(vm.hfsVmId);
        if (hfsVm.status.equals("ACTIVE") && (!troubleshootVmService.getHfsAgentStatus(vm.hfsVmId).equals("OK"))) {
            throw new Vps4Exception("AGENT_DOWN","Agent for vmId " + vm.vmId + " is down. Refusing to take snapshot.");
        }
    }

    private void validateVmCanCreateSnapshot(UUID orionGuid, UUID backupJobId, String name, SnapshotType snapshotType) {
        validateUserIsShopper(user);
        validateIfSnapshotOverQuota(snapshotService, orionGuid, snapshotType);
        validateNoOtherSnapshotsInProgress(snapshotService, orionGuid);
        validateSnapshotName(name);
        validateSnapshotNotPaused(schedulerWebService, backupJobId, snapshotType);
    }

    private void validateVmCanCreateCephSnapshot(SnapshotRequest snapshotRequest, VirtualMachine vm) {
        validateDCLimit(snapshotRequest);
        validateHVLimit(snapshotRequest, vm.hfsVmId);
    }

    private Action createSnapshotAndActionEntries(VirtualMachine vm, String snapshotName, SnapshotType snapshotType) {
        UUID snapshotId = snapshotService.createSnapshot(vm.projectId, vm.vmId, snapshotName, snapshotType);
        long actionId = actionService.createAction(
                snapshotId, ActionType.CREATE_SNAPSHOT, new JSONObject().toJSONString(), user.getUsername());
        logger.info("Creating db entries for snapshot creation. Snapshot Action ID: {}, Snapshot ID: {}", actionId, snapshotId);
        return actionService.getAction(actionId);
    }

    private void validateDCLimit(SnapshotRequest snapshotRequest) {
        if(!snapshotRequest.snapshotType.equals((SnapshotType.AUTOMATIC)))
            return;
        int currentLoad = snapshotService.totalSnapshotsInProgress();
        int maxAllowed = Integer.parseInt(config.get("vps4.autobackup.concurrentLimit", "30"));
        if (currentLoad >= maxAllowed) {
            logger.info("Cannot create snapshot for vmID {} because of too many concurrent snapshots", snapshotRequest.vmId);
            throw new Vps4Exception("SNAPSHOT_DC_LIMIT_REACHED",
                String.format("Too many concurrent snapshots. Cannot create snapshot for vmID %s now", snapshotRequest.vmId));
        }
    }

    private void validateHVLimit(SnapshotRequest snapshotRequest, long hfsVmId) {
        if (!Boolean.parseBoolean(config.get("vps4.autobackup.checkHvConcurrentLimit")))
            return;
        if (!snapshotRequest.snapshotType.equals((SnapshotType.AUTOMATIC)))
            return;
        String hypervisorHostname = getVmHypervisorHostname(snapshotRequest.vmId, hfsVmId);
        if (hypervisorHostname != null) {
            UUID conflictingVmId = snapshotService.getVmIdWithInProgressSnapshotOnHv(hypervisorHostname);
            if (conflictingVmId != null) { // limit 1 snapshot per HV
                logger.info("Cannot create snapshot for vmID {} because another vmId {} already has snapshot in progress on {}",
                            snapshotRequest.vmId, conflictingVmId, hypervisorHostname);
                throw new Vps4Exception("SNAPSHOT_HV_LIMIT_REACHED", "Only 1 snapshot allowed per hypervisor");
            }
            snapshotService.saveVmHvForSnapshotTracking(snapshotRequest.vmId, hypervisorHostname);
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
    @Path("/{snapshotId}")
    @JsonView(Views.Public.class)
    public Snapshot getSnapshot(@PathParam("snapshotId") UUID snapshotId) {
        Snapshot snapshot = snapshotService.getSnapshot(snapshotId);

        validateSnapshotExists(snapshotId, snapshot, user);
        vmResource.getVm(snapshot.vmId); // auth validation

        return snapshot;
    }

    @DELETE
    @Path("/{snapshotId}")
    public SnapshotAction destroySnapshot(@PathParam("snapshotId") UUID snapshotId) {
        Snapshot snapshot = getSnapshot(snapshotId);
        snapshotService.deleteVmHvForSnapshotTracking(snapshot.vmId);

        // Note: this doesn't do anything on the OpenStack side, just updates our database
        cancelIncompleteSnapshotActions(snapshotId);

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

    private void cancelIncompleteSnapshotActions(UUID snapshotId) {
        List<Action> actions = actionService.getIncompleteActions(snapshotId);
        for (Action action: actions) {
            snapshotActionResource.cancelSnapshotAction(snapshotId, action.id);
        }
    }

    @GET
    @Path("/{snapshotId}/withDetails")
    @JsonView(Views.Internal.class)
    public Snapshot getSnapshotWithDetails(@PathParam("snapshotId") UUID snapshotId) {
        return getSnapshot(snapshotId);
    }


    public static class SnapshotRenameRequest {
        public String name;
    }

    @PATCH
    @Path("/{snapshotId}")
    public SnapshotAction renameSnapshot(@PathParam("snapshotId") UUID snapshotId, SnapshotRenameRequest request) {

        Snapshot snapshot = getSnapshot(snapshotId);
        validateSnapshotName(request.name);
        String notes = String.format("Old name = %s, New Name = %s", snapshot.name, request.name);

        long actionId = actionService.createAction(snapshotId,  ActionType.RENAME_SNAPSHOT,  new JSONObject().toJSONString(), user.getUsername());

        snapshotService.renameSnapshot(snapshotId, request.name);
        actionService.completeAction(actionId, new JSONObject().toJSONString(), notes);

        return new SnapshotAction(actionService.getAction(actionId), user.isEmployee());
    }
}
