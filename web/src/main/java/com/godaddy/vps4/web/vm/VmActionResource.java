package com.godaddy.vps4.web.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionType;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.web.PaginatedResult;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.security.GDUser;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class VmActionResource {

    private static final Logger logger = LoggerFactory.getLogger(VmActionResource.class);

    private final PrivilegeService privilegeService;
    private final ActionService actionService;
    private final Vps4UserService userService;
    private final CommandService commandService;
    private final GDUser user;

    @Inject
    public VmActionResource(PrivilegeService privilegeService, ActionService actionService,
            Vps4UserService userService, CommandService commandService, GDUser user) {
        this.privilegeService = privilegeService;
        this.actionService = actionService;
        this.userService = userService;
        this.commandService = commandService;
        this.user = user;
    }

    private void verifyUserPrivilege(UUID vmId) {
        Vps4User vps4User = userService.getOrCreateUserForShopper(user.getShopperId());
        privilegeService.requireAnyPrivilegeToVmId(vps4User, vmId);
    }

    @GET
    @Path("{vmId}/actions")
    public PaginatedResult<VmAction> getActions(
        @PathParam("vmId") UUID vmId,
        @DefaultValue("10") @QueryParam("limit") long limit,
        @DefaultValue("0") @QueryParam("offset") long offset,
        @ApiParam(value = "A list of status to filter the actions by. This parameter is incompatible with 'actionType'", required = false) @QueryParam("status") List<String> status,
        @ApiParam(value = "A type of action to filter the actions by. This parameter is incompatible with 'status'", required = false) @QueryParam("actionType") ActionType actionType,
        @Context UriInfo uri) {

        if (user.isShopper())
            verifyUserPrivilege(vmId);

        ResultSubset<Action> actions;
        // For now we will support listing by either actionType or actionStatus but not both
        if (actionType != null) {
            actions = actionService.getActions(vmId, limit, offset, actionType);
        }
        else {
            actions = actionService.getActions(vmId, limit, offset, status);
        }

        long totalRows = 0;
        List<VmAction> vmActionList = new ArrayList<>();
        if (actions != null) {
            totalRows = actions.totalRows;
            vmActionList = actions.results
                    .stream()
                    .map(VmAction::new)
                    .collect(Collectors.toList());
        }

        return new PaginatedResult<>(vmActionList, limit, offset, totalRows, uri);
    }

    private Action getVmActionFromCore(UUID vmId, long actionId) {
        if (user.isShopper())
            verifyUserPrivilege(vmId);

        Action action = actionService.getAction(vmId, actionId);
        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }

        return action;
    }

    @GET
    @Path("{vmId}/actions/{actionId}")
    public VmAction getVmAction(@PathParam("vmId") UUID vmId, @PathParam("actionId") long actionId) {
        return new VmAction(this.getVmActionFromCore(vmId, actionId));
    }

    @AdminOnly
    @GET
    @Path("{vmId}/actions/{actionId}/withDetails")
    public VmActionWithDetails getVmActionWithDetails(@PathParam("vmId") UUID vmId,
                                                      @PathParam("actionId") long actionId) {
        Action action = this.getVmActionFromCore(vmId, actionId);
        CommandState commandState = this.commandService.getCommand(action.commandId);
        return new VmActionWithDetails(action, commandState);
    }
}