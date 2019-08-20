package com.godaddy.vps4.web.snapshot;

import static com.godaddy.vps4.web.util.RequestValidation.validateSnapshotExists;
import static com.godaddy.vps4.web.util.RequestValidation.verifyUserPrivilegeToProject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
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

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.snapshot.SnapshotActionService;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.security.TemporarilyDisabled;
import com.godaddy.vps4.web.util.Commands;
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

    private static final Logger logger = LoggerFactory.getLogger(SnapshotActionResource.class);

    private final ActionService actionService;
    private final CommandService commandService;
    private final GDUser user;
    private final PrivilegeService privilegeService;
    private final SnapshotService snapshotService;
    private final Vps4UserService userService;
    private final Map<ActionType, String> actionTypeToCancelCmdNameMap;

    @Inject
    public SnapshotActionResource(@SnapshotActionService ActionService actionService,
                                  PrivilegeService privilegeService, SnapshotService snapshotService,
                                  Vps4UserService userService, CommandService commandService,
                                  GDUser user, Map<ActionType, String> actionTypeToCancelCmdNameMap) {
        this.actionService = actionService;
        this.commandService = commandService;
        this.privilegeService = privilegeService;
        this.snapshotService = snapshotService;
        this.userService = userService;
        this.user = user;
        this.actionTypeToCancelCmdNameMap = actionTypeToCancelCmdNameMap;
    }

    private void verifyPrivilege(UUID snapshotId) {
        Snapshot snapshot = snapshotService.getSnapshot(snapshotId);
        validateSnapshotExists(snapshotId, snapshot, user);

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

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/{snapshotId}/actions/{actionId}/withDetails")
    public SnapshotActionWithDetails getSnapshotActionWithDetails(
            @PathParam("snapshotId") UUID snapshotId,
            @PathParam("actionId") long actionId) {
        Action action = this.getSnapshotActionFromCore(snapshotId, actionId);
        CommandState commandState = this.commandService.getCommand(action.commandId);
        return new SnapshotActionWithDetails(action, commandState, user.isEmployee());
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @TemporarilyDisabled
    @POST
    @Path("{snapshotId}/actions/{actionId}/cancel")
    public void cancelSnapshotAction(
        @PathParam("snapshotId") UUID snapshotId,
        @PathParam("actionId") long actionId) {
        verifyPrivilege(snapshotId);
        Action action = this.getSnapshotActionFromCore(snapshotId, actionId);
        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }

        if (!canCancel(action)) {
            throw new Vps4Exception("INVALID_STATUS", "This snapshot action cannot be cancelled");
        }

        logger.info("Cancel request received for snapshot action {}", actionId);
        if (action.commandId != null) {
            Commands.cancel(commandService, action.commandId);
        }
        String note = String.format("Snapshot action cancelled via api by %s", user.getUsername());
        if (shouldQueueRollbackCommand(action.type)) {
            UUID commandId = queueRollbackCommand(action);
            note = String.format("%s. Async cleanup queued: %s", note, commandId.toString());
        }

        actionService.cancelAction(actionId, new JSONObject().toJSONString(), note);
    }

    private boolean shouldQueueRollbackCommand(ActionType actionType) {
        return actionTypeToCancelCmdNameMap.containsKey(actionType);
    }

    private UUID queueRollbackCommand(Action action) {
        String cancelCommandName = actionTypeToCancelCmdNameMap.get(action.type);
        // Right now the assumption is that a cancel command implementation only takes the action id as
        // the request input
        CommandState command =  Commands.execute(commandService, cancelCommandName, action.id);
        logger.info("Queued cancel processing for snapshot action {} using command {}", action.id, command.commandId);
        return command.commandId;
    }

    private boolean canCancel(Action action) {
        return action.status.equals(ActionStatus.NEW) || action.status.equals(ActionStatus.IN_PROGRESS);
    }
}
