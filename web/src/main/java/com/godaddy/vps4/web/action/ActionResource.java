package com.godaddy.vps4.web.action;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Vps4Api
@Api(tags = { "actions" })

@Path("/api/actions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActionResource {

    private final ActionService actionService;

    @Inject
    public ActionResource(ActionService actionService) {
        this.actionService = actionService;
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})@GET
    @Path("/vm/actions")
    @ApiOperation(value = "Find all vm actions that match the filters specified. Actions are sorted descending by create date.",
            notes = "Find all vm actions that match the filters specified. Actions are sorted descending by create date.")
    public List<Action> getVmActions(@QueryParam("vmId") UUID vmId,
                                     @QueryParam("actionType") ActionType actionType,
                                     @ApiParam(value = "the maximum number of actions to return") @QueryParam("limit") long limit,
                                     @ApiParam(value = "the number of actions to offset from the most recent") @QueryParam("offset") long offset,
                                     @QueryParam("status") ActionStatus status,
                                     @ApiParam(value = "begin date in GMT, Example: 2007-12-03T10:15:30.00Z") @QueryParam("beginDate") String beginDate,
                                     @ApiParam(value = "end date in GMT, Example: 2007-12-03T10:15:30.00Z") @QueryParam("endDate") String endDate) {
        List<String> statusList = new ArrayList<>();

        if(status != null) {
            statusList.add(status.toString());
        }

        Instant end = validateDate(endDate);
        Instant begin = validateDate(beginDate);

        ResultSubset<Action> resultSubset = actionService.getActions(vmId, limit, offset, statusList, begin, end, actionType);
        return resultSubset==null?new ArrayList<>():resultSubset.results;
    }

    private Instant validateDate(String dateToValidate) {
        Instant date = null;
        if(dateToValidate != null) {
            try {
                date = Instant.parse(dateToValidate);
            } catch (DateTimeParseException e) {
                throw new Vps4Exception("DATE_NOT_VALID", dateToValidate + " is not a valid java Instant");
            }
        }
        return date;
    }
}
