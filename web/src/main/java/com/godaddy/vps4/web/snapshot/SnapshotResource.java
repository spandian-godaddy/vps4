package com.godaddy.vps4.web.snapshot;

import static com.godaddy.vps4.web.util.RequestValidation.validateIfSnapshotOverQuota;
import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotName;
import static com.godaddy.vps4.web.util.RequestValidation.ensureHasShopperAccess;
import static com.godaddy.vps4.web.util.RequestValidation.verifyUserPrivilegeToProject;

import com.godaddy.vps4.orchestration.snapshot.Vps4DestroySnapshot;
import com.godaddy.vps4.orchestration.snapshot.Vps4SnapshotVm;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.*;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4NoShopperException;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.vm.VmResource;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final PrivilegeService privilegeService;
    private final SnapshotService snapshotService;
    private final VirtualMachineService virtualMachineService;
    private final VmResource vmResource;
    private final Vps4UserService userService;

    @Inject
    public SnapshotResource(@SnapshotActionService ActionService actionService, CommandService commandService,
                            GDUser user, PrivilegeService privilegeService, SnapshotService snapshotService,
                            VirtualMachineService virtualMachineService, VmResource vmResource,
                            Vps4UserService userService) {
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.privilegeService = privilegeService;
        this.snapshotService = snapshotService;
        this.virtualMachineService = virtualMachineService;
        this.vmResource = vmResource;
        this.userService = userService;
    }


    @GET
    @Path("/")
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

    @POST
    @Path("/")
    public SnapshotAction createSnapshot(SnapshotRequest snapshotRequest) {
        VirtualMachine vm = vmResource.getVm(snapshotRequest.vmId);
        validateCreation(vm.orionGuid, snapshotRequest.name);
        Action action = createSnapshotAndActionEntries(vm, snapshotRequest.name);
        kickoffSnapshotCreation(vm.vmId, snapshotRequest.name, vm.hfsVmId, action, vm.orionGuid);
        return new SnapshotAction(actionService.getAction(action.id));
    }

    public static class SnapshotRequest {
        public String name;
        public UUID vmId;
    }

    private void validateCreation(UUID orionGuid, String name) {
        ensureHasShopperAccess(user);
        validateIfSnapshotOverQuota(snapshotService, orionGuid);
        validateSnapshotName(name);
    }

    private Action createSnapshotAndActionEntries(VirtualMachine vm, String snapshotName) {
        long vps4UserId = virtualMachineService.getUserIdByVmId(vm.vmId);
        UUID snapshotId = snapshotService.createSnapshot(vm.projectId, vm.vmId, snapshotName);
        long actionId = actionService.createAction(
                snapshotId, ActionType.CREATE_SNAPSHOT, new JSONObject().toJSONString(), vps4UserId);
        logger.info("Creating db entries for snapshot creation. Snapshot Action ID: {}, Snapshot ID: {}", actionId, snapshotId);
        return actionService.getAction(actionId);
    }

    private void kickoffSnapshotCreation(UUID vmId, String snapshotName,
                                         long hfsVmId, Action action, UUID orionGuid) {
        UUID snapshotId = action.resourceId; // the resourceId refers to the associated snapshotId
        Vps4SnapshotVm.Request commandRequest = new Vps4SnapshotVm.Request();
        commandRequest.hfsVmId = hfsVmId;
        commandRequest.snapshotName = snapshotName;
        commandRequest.vps4SnapshotId = snapshotId;
        commandRequest.actionId = action.id;
        commandRequest.orionGuid = orionGuid;
        commandRequest.vps4UserId = action.vps4UserId;

        CommandState command = Commands.execute(
                commandService, actionService, "Vps4SnapshotVm", commandRequest);
        logger.info(
                "Creating snapshot {}:{} for vps4 vm {} with command {}:{}",
                snapshotId, snapshotName, vmId, action.id, command.commandId
        );
    }

    @GET
    @Path("/{snapshotId}")
    public Snapshot getSnapshot(@PathParam("snapshotId") UUID snapshotId) {
        Snapshot snapshot = snapshotService.getSnapshot(snapshotId);
        verifyPrivilege(snapshot);

        return snapshot;
    }

    @DELETE
    @Path("/{snapshotId}")
    public SnapshotAction destroySnapshot(@PathParam("snapshotId") UUID snapshotId) {
        SnapshotWithDetails snapshot = getSnapshotWithDetails(snapshotId);

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
    public SnapshotWithDetails getSnapshotWithDetails(@PathParam("snapshotId") UUID snapshotId) {
        SnapshotWithDetails snapshot = snapshotService.getSnapshotWithDetails(snapshotId);
        verifyPrivilege(snapshot);

        return snapshot;
    }

    private void verifyPrivilege(Snapshot snapshot) {
        if (snapshot == null || snapshot.status == SnapshotStatus.DESTROYED)
            throw new NotFoundException("Unknown snapshot");

        if (user.isShopper())
            verifyUserPrivilegeToProject(userService, privilegeService, user.getShopperId(), snapshot.projectId);
    }
}
