package com.godaddy.vps4.web.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
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
    public PaginatedResult<Action> getActions(@PathParam("vmId") UUID vmId,
            @DefaultValue("10") @QueryParam("limit") long limit,
            @DefaultValue("0") @QueryParam("offset") long offset,
            @QueryParam("status") List<String> status,
            @Context UriInfo uri) {

        if (user.isShopper())
            verifyUserPrivilege(vmId);

        ResultSubset<Action> actions;
        actions = actionService.getActions(vmId, limit, offset, status);

        long totalRows = 0;
        List<Action> actionList = new ArrayList<Action>();
        if (actions != null) {
            totalRows = actions.totalRows;
            actionList = actions.results;
        }

        return new PaginatedResult<Action>(actionList, limit, offset, totalRows, uri);
    }

    @GET
    @Path("{vmId}/actions/{actionId}")
    public Action getVmAction(@PathParam("vmId") UUID vmId, @PathParam("actionId") long actionId) {
        if (user.isShopper())
            verifyUserPrivilege(vmId);

        Action action = actionService.getVmAction(vmId, actionId);
        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }

        return action;
    }

    @AdminOnly
    @GET
    @Path("{vmId}/actions/{actionId}/withDetails")
    public ActionWithDetails getVmActionWithDetails(@PathParam("vmId") UUID vmId,
            @PathParam("actionId") long actionId) {
        Action action = this.getVmAction(vmId, actionId);
        CommandState commandState = this.commandService.getCommand(action.commandId);
        return new ActionWithDetails(action, commandState);
    }
}