package com.godaddy.vps4.web.appmonitors;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
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
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionService.ActionListFilters;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
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
public class VmActionsMonitorResource {

    private final MonitorService monitorService;
    private final ActionService actionService;
    private final VirtualMachineService virtualMachineService;

    @Inject
    public VmActionsMonitorResource(MonitorService monitorService, ActionService actionService, VirtualMachineService virtualMachineService) {
        this.monitorService = monitorService;
        this.actionService = actionService;
        this.virtualMachineService = virtualMachineService;
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/pending/provision")
    @ApiOperation(value = "Find all VM id's that are pending provisioning for longer than m minutes, default 60 minutes",
            notes = "Find all VM id's that are pending provisioning for longer than m minutes, default 60 minutes")
    public List<VmActionData> getProvisioningPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("60") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(thresholdInMinutes, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/pending/startvm")
    @ApiOperation(value = "Find all VM id's that are pending start vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending start vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getStartPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        return monitorService.getVmsByActions( thresholdInMinutes, ActionType.START_VM, ActionStatus.IN_PROGRESS);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/pending/stopvm")
    @ApiOperation(value = "Find all VM id's that are pending stop vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending stop vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getStopPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(thresholdInMinutes, ActionType.STOP_VM, ActionStatus.IN_PROGRESS);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/pending/restartvm")
    @ApiOperation(value = "Find all VM id's that are pending restart vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending restart vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getRestartPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        ActionListFilters actionListFilters = new ActionListFilters();
        actionListFilters.byType(ActionType.RESTART_VM, ActionType.POWER_CYCLE);
        actionListFilters.byStatus(ActionStatus.IN_PROGRESS);
        actionListFilters.byDateRange(null, Instant.now().minus(Duration.ofMinutes(thresholdInMinutes)));

        ResultSubset<Action> resultSubset = actionService.getActionList(actionListFilters);
        if(resultSubset == null) {
            return new ArrayList<>();
        }
        List<Action> actions = resultSubset.results;
        return mapActionsToVmActionData(actions);
    }

    private List<VmActionData> mapActionsToVmActionData(List<Action> actions) {
        if(actions.isEmpty()) {
            return  new ArrayList<>();
        }
        List<VmActionData> actionData = new ArrayList<>(actions.size());
        actions.forEach(action -> {
            actionData.add(new VmActionData(Long.toString(action.id), action.commandId, action.resourceId, action.type.toString()));
        });
        return actionData;
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/pending/backupactions")
    @ApiOperation(value = "Find all snapshot ids that are pending backup vm action for longer than m minutes, default 2 hours",
            notes = "Find all snapshot id's that are pending backup vm action for longer than m minutes, default 2 hours")
    public List<SnapshotActionData> getBackupPendingActions(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsBySnapshotActions(thresholdInMinutes, ActionStatus.IN_PROGRESS, ActionStatus.NEW, ActionStatus.ERROR);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/pending/restorevm")
    @ApiOperation(value = "Find all VM id's that are pending restore vm action for longer than m minutes, default 2 hours",
            notes = "Find all VM id's that are pending restore vm action for longer than m minutes, default 2 hours")
    public List<VmActionData> getRestorePendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(thresholdInMinutes, ActionType.RESTORE_VM, ActionStatus.IN_PROGRESS);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/pending/newactions")
    @ApiOperation(value = "Find all vm actions pending in new status for longer than m minutes, default 2 hours",
            notes = "Find all VM actions that are pending in new status for longer than m minutes, default 2 hours")
    public List<VmActionData> getVmsWithPendingNewActions(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsByActionStatus(thresholdInMinutes, ActionStatus.NEW);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/pending/allactions")
    @ApiOperation(value = "Find all vm actions in pending 'in_progress' status for longer than m minutes, default 2 hours",
            notes = "Find all VM actions that are in pending 'in_progress' status for longer than m minutes, default 2 hours")
    public List<VmActionData> getVmsWithAllPendingActions(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsByActionStatus(thresholdInMinutes, ActionStatus.IN_PROGRESS);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/missing_backup_jobs")
    @ApiOperation(value = "Find all active vms that do not have a backup job id, meaning scheduler create job failed",
            notes = "Find all active vms that do not have a backup job id, meaning scheduler create job failed")
    public List<BackupJobAuditData> getVmsWithoutBackupJob() {
        return monitorService.getVmsFilteredByNullBackupJob();
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
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

            ResultSubset<Action> resultSubset = actionService.getActionList(actionFilters);
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

    @RequiresRole(roles = { GDUser.Role.ADMIN })
    @POST
    @Path("/checkpoints/{actionType}")
    public MonitoringCheckpoint setMonitoringCheckpoint(@PathParam("actionType") ActionType actionType) {
         return monitorService.setMonitoringCheckpoint(actionType);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/checkpoints/{actionType}")
    public MonitoringCheckpoint getMonitoringCheckpoint(@PathParam("actionType") ActionType actionType) {
        return monitorService.getMonitoringCheckpoint(actionType);
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @GET
    @Path("/checkpoints")
    public List<MonitoringCheckpoint> getMonitoringCheckpoints() {
        return monitorService.getMonitoringCheckpoints();
    }

    @RequiresRole(roles = {GDUser.Role.ADMIN})
    @DELETE
    @Path("/checkpoints/{actionType}")
    public void deleteMonitoringCheckpoint(@PathParam("actionType") ActionType actionType) {
        monitorService.deleteMonitoringCheckpoint(actionType);
    }
}
