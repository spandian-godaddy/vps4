package com.godaddy.vps4.web.vm;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnDateInstant;
import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.util.ActionListFilters;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.PaginatedResult;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.GDUser.Role;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.web.util.RequestValidation;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

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
    private final Map<ActionType, String> actionTypeToCancelCmdNameMap;

    @Inject
    public VmActionResource(PrivilegeService privilegeService, ActionService actionService,
                            Vps4UserService userService, CommandService commandService, GDUser user,
                            Map<ActionType, String> actionTypeToCancelCmdNameMap) {
        this.privilegeService = privilegeService;
        this.actionService = actionService;
        this.userService = userService;
        this.commandService = commandService;
        this.user = user;
        this.actionTypeToCancelCmdNameMap = actionTypeToCancelCmdNameMap;
    }

    @GET
    @Path("{vmId}/actions")
    public PaginatedResult<VmAction> getVmActionList(
            @PathParam("vmId") UUID vmId,
            @ApiParam(value = "A list of status to filter the actions.") @QueryParam("status") List<String> statusList,
            @ApiParam(value = "A list of actions to filter the actions.") @QueryParam("actionType") List<String> typeList,
            @ApiParam(value = "begin date in UTC, Example: 2007-12-03T10:15:30.00Z") @QueryParam("beginDate") String beginDate,
            @ApiParam(value = "end date in UTC, Example: 2007-12-03T10:15:30.00Z") @QueryParam("endDate") String endDate,
            @ApiParam(value = "the maximum number of actions to return") @DefaultValue("10") @QueryParam("limit") long limit,
            @ApiParam(value = "the number of actions to offset from the most recent") @DefaultValue("0") @QueryParam("offset") long offset,
            @Context UriInfo uri) {

        if (user.isShopper())
            RequestValidation.verifyUserPrivilegeToVm(userService, privilegeService, user.getShopperId(), vmId);

        Instant start = validateAndReturnDateInstant(beginDate);
        Instant end = validateAndReturnDateInstant(endDate);

        List<ActionStatus> enumStatusList = statusList.stream()
                .map(s -> validateAndReturnEnumValue(ActionStatus.class, s))
                .collect(Collectors.toList());

        List<ActionType> enumTypeList = typeList.stream()
                .map(t -> validateAndReturnEnumValue(ActionType.class, t))
                .collect(Collectors.toList());


        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byResourceId(vmId);
        actionFilters.byStatus(enumStatusList);
        actionFilters.byType(enumTypeList);
        actionFilters.byDateRange(start, end);
        actionFilters.setLimit(limit);
        actionFilters.setOffset(offset);

        ResultSubset<Action> actions = actionService.getActionList(actionFilters);

        long totalRows = 0;
        List<VmAction> vmActionList = new ArrayList<>();
        if (actions != null) {
            totalRows = actions.totalRows;
            vmActionList = actions.results.stream()
                    .map(action -> new VmAction(action, user.isEmployee()))
                    .collect(Collectors.toList());
        }

        return new PaginatedResult<>(vmActionList, limit, offset, totalRows, uri);
    }

    private Action getVmActionFromCore(UUID vmId, long actionId) {
        if (user.isShopper())
            RequestValidation.verifyUserPrivilegeToVm(userService, privilegeService, user.getShopperId(), vmId);

        Action action = actionService.getAction(vmId, actionId);
        if (action == null) {
            throw new NotFoundException("actionId " + actionId + " not found");
        }

        return action;
    }

    @GET
    @Path("{vmId}/actions/{actionId}")
    public VmAction getVmAction(@PathParam("vmId") UUID vmId, @PathParam("actionId") long actionId) {
        return new VmAction(this.getVmActionFromCore(vmId, actionId), user.isEmployee());
    }

    @GET
    @Path("{vmId}/actions/{actionId}/withDetails")
    @RequiresRole(roles = {Role.ADMIN, Role.HS_AGENT, Role.HS_LEAD, Role.SUSPEND_AUTH})
    public VmActionWithDetails getVmActionWithDetails(@PathParam("vmId") UUID vmId,
                                                      @PathParam("actionId") long actionId) {
        Action action = this.getVmActionFromCore(vmId, actionId);
        CommandState commandState = action.commandId != null? this.commandService.getCommand(action.commandId) : null;
        return new VmActionWithDetails(action, commandState, user.isEmployee());
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @POST
    @Path("{vmId}/actions/{actionId}/cancel")
    public void cancelVmAction(@PathParam("vmId") UUID vmId, @PathParam("actionId") long actionId) {
        Action action = this.getVmActionFromCore(vmId, actionId);

        if (!canCancel(action)) {
            throw new Vps4Exception("INVALID_STATUS", "This action cannot be cancelled");
        }

        logger.info("Cancel request received for action {}", actionId);
        if (action.commandId != null) {
            Commands.cancel(commandService, action.commandId);
        }
        String note = "Action cancelled via api by admin";
        if (shouldQueueRollbackCommand(action.type)) {
            UUID commandId = queueRollbackCommand(action);
            note = String.format("%s. Async cleanup queued: %s", note, commandId.toString());
        }

        actionService.cancelAction(actionId, new JSONObject().toJSONString(), note);
    }

    @GET
    @Path("{vmId}/actionTypes")
    public List<String> getVmActionTypes(@PathParam("vmId") UUID vmId) {
        if (user.isShopper())
            RequestValidation.verifyUserPrivilegeToVm(userService, privilegeService, user.getShopperId(), vmId);
        return actionService.getVmActionTypes(vmId);
    }

    private boolean shouldQueueRollbackCommand(ActionType actionType) {
        return actionTypeToCancelCmdNameMap.containsKey(actionType);
    }

    private UUID queueRollbackCommand(Action action) {
        String cancelCommandName = actionTypeToCancelCmdNameMap.get(action.type);
        CommandState command =  Commands.execute(commandService, cancelCommandName, action.id);
        logger.info("Queued cancel processing for action {} using command {}", action.id, command.commandId);
        return command.commandId;
    }

    private boolean canCancel(Action action) {
        return action.status.equals(ActionStatus.NEW) || action.status.equals(ActionStatus.IN_PROGRESS);
    }
}
