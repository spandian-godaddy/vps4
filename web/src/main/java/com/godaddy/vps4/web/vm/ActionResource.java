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

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.web.PaginatedResult;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class ActionResource {

    private final PrivilegeService privilegeService;
    private final ActionService actionService;
    private final Vps4User user;

    @Inject
    public ActionResource(PrivilegeService privilegeService,
                          ActionService actionService,
                          Vps4User user){
        this.privilegeService = privilegeService;
        this.actionService = actionService;
        this.user = user;
    }

    @GET
    @Path("actions/{actionId}")
    public Action getAction(@PathParam("actionId") long actionId) {

        Action action = actionService.getAction(actionId);

        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }

        if (action.virtualMachineId == null) {
            requireSameActionUser(action);
        }
        else {
            privilegeService.requireAnyPrivilegeToVmId(user, action.virtualMachineId);
        }

        return action;
    }

    @GET
    @Path("vms/{vmId}/actions")
    public PaginatedResult<Action> getActions(@PathParam("vmId") UUID vmId,
                                              @DefaultValue("10") @QueryParam("limit") long limit,
                                              @DefaultValue("0") @QueryParam("offset") long offset,
                                              @Context UriInfo uri) {
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);

        ResultSubset<Action> actions = actionService.getActions(vmId, limit, offset);
        long totalRows = 0;
        List<Action> actionList = new ArrayList<Action>();
        if(actions != null){
            totalRows = actions.totalRows;
            actionList = actions.results;
        }

        return new PaginatedResult<Action>(actionList, limit, offset, totalRows, uri);
    }

    private void requireSameActionUser(Action action) {
        if (user.getId() != action.vps4UserId) {
            throw new AuthorizationException(user.getShopperId() + " is not authorized to view action " + action.id);
        }
    }


}
