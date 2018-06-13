package com.godaddy.vps4.web.snapshot;

import static com.godaddy.vps4.web.util.RequestValidation.verifyUserPrivilegeToProject;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "snapshots" })

@Path("/api/snapshots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class SnapshotActionResource {

    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;
    private final PrivilegeService privilegeService;
    private final SnapshotService snapshotService;
    private final Vps4UserService userService;

    @Inject
    public SnapshotActionResource(@SnapshotActionService ActionService actionService,
                                  PrivilegeService privilegeService, SnapshotService snapshotService,
                                  Vps4UserService userService, CommandService commandService, GDUser user) {
        this.actionService = actionService;
        this.commandService = commandService;
        this.privilegeService = privilegeService;
        this.snapshotService = snapshotService;
        this.userService = userService;
        this.user = user;
    }

    private void verifyPrivilege(UUID snapshotId) {
        Snapshot snapshot = snapshotService.getSnapshot(snapshotId);
        if (snapshot == null || snapshot.status == SnapshotStatus.DESTROYED || snapshot.status == SnapshotStatus.CANCELLED)
            throw new NotFoundException("Unknown snapshot ID: " + snapshotId);

        if (user.isShopper())
            verifyUserPrivilegeToProject(userService, privilegeService, user.getShopperId(), snapshot.projectId);
    }

    @GET
    @Path("/{snapshotId}/actions")
    public List<SnapshotAction> getActions(@PathParam("snapshotId") UUID snapshotId) {
        verifyPrivilege(snapshotId);
        return actionService.getActions(snapshotId)
                .stream()
                .map(action -> new SnapshotAction(action, user.isEmployee()))
                .collect(Collectors.toList());
    }

    private Action getSnapshotActionFromCore(UUID snapshotId, long actionId) {
        verifyPrivilege(snapshotId);
        Action action = actionService.getAction(snapshotId, actionId);
        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }

        return action;
    }

    @GET
    @Path("/{snapshotId}/actions/{actionId}")
    public SnapshotAction getSnapshotAction(
            @PathParam("snapshotId") UUID snapshotId,
            @PathParam("actionId") long actionId) {
        return new SnapshotAction(this.getSnapshotActionFromCore(snapshotId, actionId), user.isEmployee());
    }

    @AdminOnly
    @GET
    @Path("/{snapshotId}/actions/{actionId}/withDetails")
    public SnapshotActionWithDetails getSnapshotActionWithDetails(
            @PathParam("snapshotId") UUID snapshotId,
            @PathParam("actionId") long actionId) {
        Action action = this.getSnapshotActionFromCore(snapshotId, actionId);
        CommandState commandState = this.commandService.getCommand(action.commandId);
        return new SnapshotActionWithDetails(action, commandState, user.isEmployee());
    }
}
