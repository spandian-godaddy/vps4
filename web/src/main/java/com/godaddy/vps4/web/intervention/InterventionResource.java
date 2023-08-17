package com.godaddy.vps4.web.intervention;

import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.util.ActionListFilters;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.google.inject.Inject;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms/{vmId}/interventions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class InterventionResource {
    private final GDUser user;
    private final ActionService actionService;

    @Inject
    public InterventionResource(GDUser user, ActionService actionService) {
        this.user = user;
        this.actionService = actionService;
    }

    @POST
    @Path("/start")
    public VmAction startIntervention(@PathParam("vmId") UUID vmId, Request request) throws JsonProcessingException {
        validateNoConflictingActions(vmId, actionService, ActionType.INTERVENTION);

        ObjectMapper om = new ObjectMapper();
        long actionId = actionService.createAction(vmId,
                                                   ActionType.INTERVENTION,
                                                   om.writeValueAsString(request),
                                                   user.getUsername());
        actionService.markActionInProgress(actionId);

        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }

    static class Request {
        public String reason;
    }

    @POST
    @Path("/end")
    public VmAction endIntervention(@PathParam("vmId") UUID vmId) {
        ActionListFilters filters = new ActionListFilters()
                .byResourceId(vmId)
                .byType(ActionType.INTERVENTION)
                .byStatus(ActionStatus.IN_PROGRESS);
        ResultSubset<Action> actionList = actionService.getActionList(filters);
        if (actionList == null) {
            throw new Vps4Exception("INVALID_STATE", "No intervention is in progress");
        }

        Action action = actionList.results.get(0);
        actionService.completeAction(action.id, null, null);
        return new VmAction(actionService.getAction(action.id), user.isEmployee());
    }
}
