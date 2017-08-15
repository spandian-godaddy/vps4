package com.godaddy.vps4.web.snapshot;

import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotOverQuota;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoOtherSnapshotsInProgress;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotName;
import static com.godaddy.vps4.web.util.RequestValidation.validateUserIsShopper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonView;
import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.security.Views;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
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

public class SnapshotResource {
    private static final Logger logger = LoggerFactory.getLogger(SnapshotResource.class);

    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;
    private final SnapshotService snapshotService;
    private final VirtualMachineService virtualMachineService;
    private final VmResource vmResource;
    private final Vps4UserService userService;

    @Inject
    public SnapshotResource(@SnapshotActionService ActionService actionService, CommandService commandService,
                            GDUser user, SnapshotService snapshotService,
                            VirtualMachineService virtualMachineService, VmResource vmResource,
                            Vps4UserService userService) {
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.snapshotService = snapshotService;
        this.virtualMachineService = virtualMachineService;
        this.vmResource = vmResource;
        this.userService = userService;
    }

    @AdminOnly
    @GET
    @Path("/")
    @JsonView(Views.Public.class)
    public List<Snapshot> getSnapshotsForUser() {
        if (user.getShopperId() == null)
            throw new Vps4NoShopperException();
        Vps4User vps4User = userService.getOrCreateUserForShopper(user.getShopperId());

        List<Snapshot> snapshots = snapshotService.getSnapshotsForUser(vps4User.getId());
        return snapshots
                .stream()
                .filter(snapshot -> snapshot.status != SnapshotStatus.DESTROYED)
                .collect(Collectors.toList());
    }

    @AdminOnly
    @POST
    @Path("/")
    public SnapshotAction createSnapshot(SnapshotRequest snapshotRequest) {
        VirtualMachine vm = vmResource.getVm(snapshotRequest.vmId);
        validateCreation(vm.orionGuid, vm.vmId, snapshotRequest.name, snapshotRequest.snapshotType);
        Action action = createSnapshotAndActionEntries(vm, snapshotRequest.name, snapshotRequest.snapshotType);
        kickoffSnapshotCreation(vm.vmId, snapshotRequest.name, vm.hfsVmId, action, vm.orionGuid, snapshotRequest.snapshotType);
        return new SnapshotAction(actionService.getAction(action.id));
    }

    public static class SnapshotRequest {
        public String name;
        public UUID vmId;
        public SnapshotType snapshotType;
    }

    private void validateCreation(UUID orionGuid, UUID vmId, String name, SnapshotType snapshotType) {
        validateUserIsShopper(user);
        validateIfSnapshotOverQuota(snapshotService, orionGuid, snapshotType);
        validateNoOtherSnapshotsInProgress(snapshotService, orionGuid);
        validateSnapshotName(name);
    }

    private Action createSnapshotAndActionEntries(VirtualMachine vm, String snapshotName, SnapshotType snapshotType) {
        long vps4UserId = virtualMachineService.getUserIdByVmId(vm.vmId);
        UUID snapshotId = snapshotService.createSnapshot(vm.projectId, vm.vmId, snapshotName, snapshotType);
        long actionId = actionService.createAction(
                snapshotId, ActionType.CREATE_SNAPSHOT, new JSONObject().toJSONString(), vps4UserId);
        logger.info("Creating db entries for snapshot creation. Snapshot Action ID: {}, Snapshot ID: {}", actionId, snapshotId);
        return actionService.getAction(actionId);
    }

    private void kickoffSnapshotCreation(UUID vmId, String snapshotName, long hfsVmId,
            Action action, UUID orionGuid, SnapshotType snapshotType) {
        UUID snapshotId = action.resourceId; // the resourceId refers to the associated snapshotId
        Vps4SnapshotVm.Request commandRequest = new Vps4SnapshotVm.Request();
        commandRequest.hfsVmId = hfsVmId;
        commandRequest.snapshotName = snapshotName;
        commandRequest.vps4SnapshotId = snapshotId;
        commandRequest.actionId = action.id;
        commandRequest.orionGuid = orionGuid;
        commandRequest.vps4UserId = action.vps4UserId;
        commandRequest.snapshotType = snapshotType;

        CommandState command = Commands.execute(
                commandService, actionService, "Vps4SnapshotVm", commandRequest);
        logger.info(
                "Creating snapshot {}:{} for vps4 vm {} with command {}:{}",
                snapshotId, snapshotName, vmId, action.id, command.commandId
        );
    }

    @AdminOnly
    @GET
    @Path("/{snapshotId}")
    @JsonView(Views.Public.class)
    public Snapshot getSnapshot(@PathParam("snapshotId") UUID snapshotId) {
        Snapshot snapshot = snapshotService.getSnapshot(snapshotId);
        if (snapshot == null || snapshot.status == SnapshotStatus.DESTROYED)
            throw new NotFoundException("Unknown snapshot");

        vmResource.getVm(snapshot.vmId);  // auth/vmcredit validation TODO:refactor
        return snapshot;
    }

    @AdminOnly
    @DELETE
    @Path("/{snapshotId}")
    public SnapshotAction destroySnapshot(@PathParam("snapshotId") UUID snapshotId) {
        Snapshot snapshot = getSnapshot(snapshotId);

        long vps4UserId = virtualMachineService.getUserIdByVmId(snapshot.vmId);
        long actionId = actionService.createAction(snapshotId,  ActionType.DESTROY_SNAPSHOT,
                new JSONObject().toJSONString(), vps4UserId);

        Vps4DestroySnapshot.Request request = new Vps4DestroySnapshot.Request();
        request.hfsSnapshotId = snapshot.hfsSnapshotId;
        request.vps4SnapshotId = snapshot.id;
        request.actionId = actionId;

        CommandState command = Commands.execute(commandService, actionService, "Vps4DestroySnapshot", request);
        logger.info("Destroying snapshot {}:{} for vps4 vm {} with command {}:{}",
                snapshotId, snapshot.name, snapshot.vmId, actionId, command.commandId);

        return new SnapshotAction(actionService.getAction(actionId));
    }

    @AdminOnly
    @GET
    @Path("/{snapshotId}/withDetails")
    @JsonView(Views.Internal.class)
    public Snapshot getSnapshotWithDetails(@PathParam("snapshotId") UUID snapshotId) {
        return getSnapshot(snapshotId);
    }


    public static class SnapshotRenameRequest {
        public String name;
    }

    @AdminOnly
    @PATCH
    @Path("/{snapshotId}")
    public SnapshotAction renameSnapshot(@PathParam("snapshotId") UUID snapshotId,
            SnapshotRenameRequest request) {

        Snapshot snapshot = getSnapshot(snapshotId);
        validateSnapshotName(request.name);
        String notes = String.format("Old name = %s, New Name = %s", snapshot.name, request.name);

        long vps4UserId = virtualMachineService.getUserIdByVmId(snapshot.vmId);
        long actionId = actionService.createAction(snapshotId,  ActionType.RENAME_SNAPSHOT,  new JSONObject().toJSONString(), vps4UserId);

        snapshotService.renameSnapshot(snapshotId, request.name);
        actionService.completeAction(actionId, new JSONObject().toJSONString(), notes);

        return new SnapshotAction(actionService.getAction(actionId));
    }


}
