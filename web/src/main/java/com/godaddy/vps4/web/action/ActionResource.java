package com.godaddy.vps4.web.action;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnDateInstant;
import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionService.ActionListFilters;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "actions" })

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActionResource {

    private final ActionService actionService;

    @Inject
    public ActionResource(ActionService actionService) {
        this.actionService = actionService;
    }

    @GET
    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @Path("/actions")
    @ApiOperation(value = "Find all vm actions that match the filters specified. Actions are sorted descending by create date.",
            notes = "Find all vm actions that match the filters specified. Actions are sorted descending by create date.")
    public List<Action> getVmActionList(
            @QueryParam("vmId") UUID vmId,
            @ApiParam(value = "A list of status to filter the actions.") @QueryParam("status") List<String> statusList,
            @ApiParam(value = "A list of actions to filter the actions.") @QueryParam("action") List<String> typeList,
            @ApiParam(value = "begin date in UTC, Example: 2007-12-03T10:15:30.00Z") @QueryParam("beginDate") String beginDate,
            @ApiParam(value = "end date in UTC, Example: 2007-12-03T10:15:30.00Z") @QueryParam("endDate") String endDate,
            @ApiParam(value = "the maximum number of actions to return") @DefaultValue("10") @QueryParam("limit") long limit,
            @ApiParam(value = "the number of actions to offset from the most recent") @DefaultValue("0") @QueryParam("offset") long offset) {

        Instant start = validateAndReturnDateInstant(beginDate);
        Instant end = validateAndReturnDateInstant(endDate);

        List<ActionStatus> enumStatusList = statusList.stream()
                .map(s -> validateAndReturnEnumValue(ActionStatus.class, s))
                .collect(Collectors.toList());

        List<ActionType> enumTypeList = typeList.stream()
                .map(t -> validateAndReturnEnumValue(ActionType.class, t))
                .collect(Collectors.toList());

        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byVmId(vmId);
        actionFilters.byStatus(enumStatusList);
        actionFilters.byType(enumTypeList);
        actionFilters.byDateRange(start, end);
        actionFilters.setLimit(limit);
        actionFilters.setOffset(offset);

        ResultSubset<Action> resultSubset = actionService.getActionList(actionFilters);
        return resultSubset==null ? new ArrayList<>() : resultSubset.results;
    }
}
