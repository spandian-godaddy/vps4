package com.godaddy.vps4.web.vm;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.jdbc.AuthorizationException;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
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
    public ActionResource(PrivilegeService privilegeService, ActionService actionService, Vps4User user) {
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
        } else {
            privilegeService.requireAnyPrivilegeToVmId(user, action.virtualMachineId);
        }

        return action;
    }

    private void requireSameActionUser(Action action) {
        if (user.getId() != action.vps4UserId) {
            throw new AuthorizationException(user.getShopperId() + " is not authorized to view action " + action.id);
        }
    }

}
