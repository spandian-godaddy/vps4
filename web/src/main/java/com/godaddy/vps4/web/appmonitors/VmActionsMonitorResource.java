package com.godaddy.vps4.web.appmonitors;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.appmonitors.BackupJobAuditData;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.MonitoringCheckpoint;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.util.ActionListFilters;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "appmonitors" })

@Path("/api/appmonitors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN})
public class VmActionsMonitorResource {

    private final MonitorService monitorService;
    private final ActionService vmActionService;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public VmActionsMonitorResource(MonitorService monitorService, ActionService actionService, VirtualMachineService virtualMachineService) {
        this.monitorService = monitorService;
        this.vmActionService = actionService;
        this.virtualMachineService = virtualMachineService;
    }

    private List<VmActionData> filterOverdueInProgressActionsByType(long thresholdInMinutes, ActionType... actionTypes) {
        ActionListFilters actionListFilters = new ActionListFilters();
        actionListFilters.byStatus(ActionStatus.IN_PROGRESS);
        actionListFilters.byType(actionTypes);
        actionListFilters.byDateRange(null, Instant.now().minus(Duration.ofMinutes(thresholdInMinutes)));

        return getFilteredActions(actionListFilters);
    }

    private List<VmActionData> filterOverdueActionsByStatus(long thresholdInMinutes, ActionStatus status) {
        ActionListFilters actionListFilters = new ActionListFilters();
        actionListFilters.byStatus(status);
        actionListFilters.byDateRange(null, Instant.now().minus(Duration.ofMinutes(thresholdInMinutes)));

        return getFilteredActions(actionListFilters);
    }

    private List<VmActionData> getFilteredActions(ActionListFilters filters) {
        ResultSubset<Action> resultSubset = vmActionService.getActionList(filters);
        return (resultSubset != null) ? mapActionsToVmActionData(resultSubset.results) : Collections.emptyList();
    }

    @GET
    @Path("/pending/provision")
    @ApiOperation(value = "Find all VM id's that are pending provisioning for longer than m minutes, default 60 minutes",
            notes = "Find all VM id's that are pending provisioning for longer than m minutes, default 60 minutes")
    public List<VmActionData> getProvisioningPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("60") long thresholdInMinutes) {
        return filterOverdueInProgressActionsByType(thresholdInMinutes, ActionType.CREATE_VM);
    }

    @GET
    @Path("/pending/startvm")
    @ApiOperation(value = "Find all VM id's that are pending start vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending start vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getStartPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") long thresholdInMinutes) {
        return filterOverdueInProgressActionsByType(thresholdInMinutes, ActionType.START_VM);
    }

    @GET
    @Path("/pending/stopvm")
    @ApiOperation(value = "Find all VM id's that are pending stop vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending stop vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getStopPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") long thresholdInMinutes) {
        return filterOverdueInProgressActionsByType(thresholdInMinutes, ActionType.STOP_VM);
    }

    @GET
    @Path("/pending/restartvm")
    @ApiOperation(value = "Find all VM id's that are pending restart vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending restart vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getRestartPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") long thresholdInMinutes) {
        return filterOverdueInProgressActionsByType(thresholdInMinutes, ActionType.RESTART_VM, ActionType.POWER_CYCLE);
    }

    private List<VmActionData> mapActionsToVmActionData(List<Action> actions) {
        List<VmActionData> actionDataList = new ArrayList<>();
        for (Action action: actions) {
            VmActionData actionData = new VmActionData();
            actionData.actionId = action.id;
            actionData.vmId = action.resourceId;
            actionData.commandId = action.commandId;
            actionData.actionType = action.type.toString();
            actionData.hfsVmId = virtualMachineService.getHfsVmIdByVmId(action.resourceId);
            actionDataList.add(actionData);
        }

        return actionDataList;
    }

    @GET
    @Path("/pending/backupactions")
    @ApiOperation(value = "Find all snapshot ids that are pending backup vm action for longer than m minutes, default 2 hours",
            notes = "Find all snapshot id's that are pending backup vm action for longer than m minutes, default 2 hours")
    public List<SnapshotActionData> getBackupPendingActions(@QueryParam("thresholdInMinutes") @DefaultValue("120") long thresholdInMinutes) {
        return monitorService.getVmsBySnapshotActions(thresholdInMinutes, ActionStatus.IN_PROGRESS, ActionStatus.NEW, ActionStatus.ERROR);
    }

    @GET
    @Path("/pending/restorevm")
    @ApiOperation(value = "Find all VM id's that are pending restore vm action for longer than m minutes, default 2 hours",
            notes = "Find all VM id's that are pending restore vm action for longer than m minutes, default 2 hours")
    public List<VmActionData> getRestorePendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("120") long thresholdInMinutes) {
        return filterOverdueInProgressActionsByType(thresholdInMinutes, ActionType.RESTORE_VM);
    }

    @GET
    @Path("/pending/newactions")
    @ApiOperation(value = "Find all vm actions pending in new status for longer than m minutes, default 2 hours",
            notes = "Find all VM actions that are pending in new status for longer than m minutes, default 2 hours")
    public List<VmActionData> getVmsWithPendingNewActions(@QueryParam("thresholdInMinutes") @DefaultValue("120") long thresholdInMinutes) {
        return filterOverdueActionsByStatus(thresholdInMinutes, ActionStatus.NEW);
    }


    @GET
    @Path("/pending/allactions")
    @ApiOperation(value = "Find all vm actions in pending 'in_progress' status for longer than m minutes, default 2 hours",
            notes = "Find all VM actions that are in pending 'in_progress' status for longer than m minutes, default 2 hours")
    public List<VmActionData> getVmsWithAllPendingActions(@QueryParam("thresholdInMinutes") @DefaultValue("120") long thresholdInMinutes) {
        return filterOverdueActionsByStatus(thresholdInMinutes, ActionStatus.IN_PROGRESS);
    }

    @GET
    @Path("/missing_backup_jobs")
    @ApiOperation(value = "Find all active vms that do not have a backup job id, meaning scheduler create job failed",
            notes = "Find all active vms that do not have a backup job id, meaning scheduler create job failed")
    public List<BackupJobAuditData> getVmsWithoutBackupJob() {
        return monitorService.getVmsFilteredByNullBackupJob();
    }

    @GET
    @Path("/failedActionsPercent")
    public List<ActionTypeErrorData> getFailedActionsForAllTypes(
            @ApiParam(value = "Number of actions to use in percentage calculation.") @DefaultValue("10") @QueryParam("windowSize") long windowSize) {
        List<ActionTypeErrorData> result = new ArrayList<>();
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.setLimit(windowSize);

        for(ActionType type : ActionType.values()) {
            MonitoringCheckpoint checkpoint = monitorService.getMonitoringCheckpoint(type);
            Instant beginDate = checkpoint == null ? null : checkpoint.checkpoint;

            actionFilters.byDateRange(beginDate, null);
            actionFilters.byType(type);

            ResultSubset<Action> resultSubset = vmActionService.getActionList(actionFilters);
            List<Action> actions = resultSubset==null?new ArrayList<>():resultSubset.results;
            List<Action> errors = actions.stream().filter(a -> a.status==ActionStatus.ERROR).collect(Collectors.toList());

            if(!errors.isEmpty()) {
                double failurePercentage = ((double) errors.size() / windowSize) * 100;
                long affectedAccounts = getCountOfAffectedAccounts(errors);
                ActionTypeErrorData actionTypeError = new ActionTypeErrorData(type, failurePercentage, affectedAccounts, errors);
                result.add(actionTypeError);
            }
        }
        return result;
    }

	private long getCountOfAffectedAccounts(List<Action> errors) {
        Set<UUID> accounts = errors.stream().map(e -> virtualMachineService.getVirtualMachine(e.resourceId).orionGuid).collect(Collectors.toSet());
        return accounts.size();
    }

    @POST
    @Path("/checkpoints/{actionType}")
    public MonitoringCheckpoint setMonitoringCheckpoint(@PathParam("actionType") ActionType actionType) {
         return monitorService.setMonitoringCheckpoint(actionType);
    }

    @GET
    @Path("/checkpoints/{actionType}")
    public MonitoringCheckpoint getMonitoringCheckpoint(@PathParam("actionType") ActionType actionType) {
        return monitorService.getMonitoringCheckpoint(actionType);
    }

    @GET
    @Path("/checkpoints")
    public List<MonitoringCheckpoint> getMonitoringCheckpoints() {
        return monitorService.getMonitoringCheckpoints();
    }

    @DELETE
    @Path("/checkpoints/{actionType}")
    public void deleteMonitoringCheckpoint(@PathParam("actionType") ActionType actionType) {
        monitorService.deleteMonitoringCheckpoint(actionType);
    }
}
