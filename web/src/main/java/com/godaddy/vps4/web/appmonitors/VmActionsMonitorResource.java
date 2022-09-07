package com.godaddy.vps4.web.appmonitors;

import static com.godaddy.vps4.web.util.RequestValidation.validateAndReturnEnumValue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

import com.godaddy.hfs.vm.Vm;
import com.godaddy.hfs.vm.VmExtendedInfo;
import com.godaddy.vps4.appmonitors.ActionCheckpoint;
import com.godaddy.vps4.appmonitors.Checkpoint;
import com.godaddy.vps4.appmonitors.HvBlockingSnapshotsData;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.ReplicationLagService;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.jdbc.DatabaseCluster;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.util.ActionListFilters;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.vm.VmDetailsResource;
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
    private final ReplicationLagService replicationLagService;
    private final DatabaseCluster databaseCluster;
    private final VmDetailsResource vmDetailsResource;

    @Inject
    public VmActionsMonitorResource(MonitorService monitorService,
                                    ActionService actionService,
                                    VirtualMachineService virtualMachineService,
                                    ReplicationLagService replicationLagService,
                                    DatabaseCluster databaseCluster,
                                    VmDetailsResource vmDetailsResource) {
        this.monitorService = monitorService;
        this.vmActionService = actionService;
        this.virtualMachineService = virtualMachineService;
        this.replicationLagService = replicationLagService;
        this.databaseCluster = databaseCluster;
        this.vmDetailsResource = vmDetailsResource;
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
    @ApiOperation(value = "Find all VM id's that are pending provisioning for longer than m minutes, default 90 minutes",
            notes = "Find all VM id's that are pending provisioning for longer than m minutes, default 90 minutes")
    public List<VmActionData> getProvisioningPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("90") long thresholdInMinutes) {
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

    @GET
    @Path("/pending/libvirtStuckVms")
    @ApiOperation(value = "Find all VM IDs that have power actions pending for longer than m minutes, default 15, or that have failed " +
                          "power actions in the last 24 hours and task state is image_snapshot, or power_state is Paused",
                  notes = "Find all VM IDs that are suspected to be stuck because of libvirt. The following commands can be used to clear" +
                          " it, replacing the dc and os_guid values" +
                          " --- @padre run playbook clear_vm_snapshot_state cloud-'dc'-ztn uuid='os_guid' desired_end_state=start ---- " +
                          "@padre run playbook unpause_vm cloud-'dc'-ztn uuid='os_guid' ---")
    public List<LibvirtStuckVm> getLibvirtStuckVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") long thresholdInMinutes) {
        List<VmActionData> vmActionList = new ArrayList<>();
        vmActionList.addAll(getRecentErroredPowerActions());
        vmActionList.addAll(filterOverdueInProgressActionsByType(thresholdInMinutes,
                                                                 ActionType.RESTART_VM,
                                                                 ActionType.STOP_VM,
                                                                 ActionType.START_VM));
        return vmActionList
                .stream()
                .map(action -> action.vmId)
                .distinct()
                .filter(vmId -> {
                    VmExtendedInfo vmExtendedInfo = vmDetailsResource.getVmExtendedDetails(vmId);
                    return vmExtendedInfo != null
                            && vmExtendedInfo.extended != null
                            && ((vmExtendedInfo.extended.taskState != null && vmExtendedInfo.extended.taskState.equalsIgnoreCase("image_snapshot"))
                            || (vmExtendedInfo.extended.powerState != null && vmExtendedInfo.extended.powerState.equalsIgnoreCase("Paused")));
                })
                .map(vmId -> {
                    Vm vm = vmDetailsResource.getMoreDetails(vmId);
                    return new LibvirtStuckVm(vmId, vm.resourceUuid);
                })
                .collect(Collectors.toList());
    }

    private List<VmActionData> getRecentErroredPowerActions() {
        ActionListFilters actionListFilters = new ActionListFilters();
        actionListFilters.byStatus(ActionStatus.ERROR);
        actionListFilters.byType(ActionType.RESTART_VM, ActionType.STOP_VM, ActionType.START_VM);
        actionListFilters.byDateRange(Instant.now().minus(1, ChronoUnit.DAYS), Instant.now());
        ResultSubset<Action> resultSubset = vmActionService.getActionList(actionListFilters);
        return (resultSubset != null) ? mapActionsToVmActionData(resultSubset.results) : Collections.emptyList();
    }

    public static class LibvirtStuckVm {
        public LibvirtStuckVm(UUID vmId, String openstackGuid) {
            this.vmId = vmId;
            this.openstackGuid = openstackGuid;
        }
        public UUID vmId;
        public String openstackGuid;
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
            actionData.created = action.created;
            actionData.initiatedBy = action.initiatedBy;
            actionDataList.add(actionData);
        }

        return actionDataList;
    }

    @GET
    @Path("/pending/backupactions")
    @ApiOperation(value = "Find all snapshot ids that are pending in progress or error status for backup vm action for longer than m minutes, default 2 hours",
            notes = "Find all snapshot id's that are pending backup vm action for longer than m minutes, default 3 hours")
    public List<SnapshotActionData> getBackupPendingActions(@QueryParam("thresholdInMinutes") @DefaultValue("180") long thresholdInMinutes) {
        return monitorService.getVmsBySnapshotActions(thresholdInMinutes,
                                                      SnapshotType.AUTOMATIC,
                                                      ActionStatus.IN_PROGRESS,
                                                      ActionStatus.ERROR);
    }

    @GET
    @Path("/pending/newbackupactions")
    @ApiOperation(value = "Find all snapshot ids that are pending in new status for backup vm action for longer than m minutes, default 2 hours",
            notes = "Find all snapshot id's that are pending backup vm action for longer than m minutes, default 5 hours")
    public List<SnapshotActionData> getNewBackupPendingActions(@QueryParam("thresholdInMinutes") @DefaultValue("300") long thresholdInMinutes) {
        return monitorService.getVmsBySnapshotActions(thresholdInMinutes, ActionStatus.NEW);
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
    @Path("/hvsBlockingSnapshots")
    @ApiOperation(value = "Get a list of hypervisors and VMs that are blocking new snapshot creations, due to entry "
            + "tracked in vm_hypervisor_snapshottracking table for over X hours, default 8 hours",
            notes = "Get a list of hypervisors and VMs that are blocking new snapshot creations, due to entry "
                    + "tracked in vm_hypervisor_snapshottracking table for over X hours, default 8 hours")
    public List<HvBlockingSnapshotsData> getHvsBlockingSnapshots(@ApiParam(value = "How many hours since the hypervisor "
            + "was inserted into vm_hypervisor_snapshottracking table") @QueryParam("thresholdInHours")
                                                                        @DefaultValue("8") long thresholdInHours) {
        return monitorService.getHvsBlockingSnapshots(thresholdInHours);
    }

    @GET
    @Path("/failedActionsPercent")
    public List<ActionTypeErrorData> getFailedActionsForAllTypes(
            @ApiParam(value = "Number of actions to use in percentage calculation.") @DefaultValue("20") @QueryParam("windowSize") long windowSize,
            @ApiParam(value = "A list of actions to filter the actions.") @DefaultValue("CREATE_VM")  @QueryParam("criticalActionType") List<String> criticalActionTypeList) {
        List<ActionTypeErrorData> result = new ArrayList<>();
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.setLimit(windowSize);

        List<ActionType> criticalActionTypeListToCheck = criticalActionTypeList.stream()
                                                .map(t -> validateAndReturnEnumValue(ActionType.class, t))
                                                .collect(Collectors.toList());

        for(ActionType type : ActionType.values()) {
            ActionCheckpoint checkpoint = monitorService.getActionCheckpoint(type);
            Instant beginDate = checkpoint == null ? null : checkpoint.checkpoint;

            actionFilters.byDateRange(beginDate, null);
            actionFilters.byType(type);

            ResultSubset<Action> resultSubset = vmActionService.getActionList(actionFilters);
            List<Action> actions = resultSubset==null?new ArrayList<>():resultSubset.results;
            List<Action> errors = actions.stream().filter(a -> a.status==ActionStatus.ERROR).collect(Collectors.toList());

            if(!errors.isEmpty()) {
                double failurePercentage = ((double) errors.size() / windowSize) * 100;
                long affectedAccounts = getCountOfAffectedAccounts(errors);
                boolean isCritical = criticalActionTypeListToCheck.contains(type);
                ActionTypeErrorData actionTypeError = new ActionTypeErrorData(type, failurePercentage, affectedAccounts, isCritical, errors);
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
    @Path("/checkpoints/actions/{actionType}")
    public ActionCheckpoint setActionCheckpoint(@PathParam("actionType") ActionType actionType) {
        return monitorService.setActionCheckpoint(actionType);
    }

    @GET
    @Path("/checkpoints/actions/{actionType}")
    public ActionCheckpoint getActionCheckpoint(@PathParam("actionType") ActionType actionType) {
        return monitorService.getActionCheckpoint(actionType);
    }

    @GET
    @Path("/checkpoints/actions")
    public List<ActionCheckpoint> getActionCheckpoints() {
        return monitorService.getActionCheckpoints();
    }

    @DELETE
    @Path("/checkpoints/actions/{actionType}")
    public void deleteActionCheckpoint(@PathParam("actionType") ActionType actionType) {
        monitorService.deleteActionCheckpoint(actionType);
    }

    @POST
    @Path("/checkpoints/{name}")
    public Checkpoint setCheckpoint(@PathParam("name") Checkpoint.Name name) {
        return monitorService.setCheckpoint(name);
    }

    @GET
    @Path("/checkpoints/{name}")
    public Checkpoint getCheckpoint(@PathParam("name") Checkpoint.Name name) {
        return monitorService.getCheckpoint(name);
    }

    @GET
    @Path("/checkpoints")
    public List<Checkpoint> getCheckpoints() {
        return monitorService.getCheckpoints();
    }

    @DELETE
    @Path("/checkpoints/{name}")
    public void deleteCheckpoint(@PathParam("name") Checkpoint.Name name) {
        monitorService.deleteCheckpoint(name);
    }

    @GET
    @Path("/incomplete/destroyvm")
    @ApiOperation(value = "Find all VM id's that are failing destroy and potentially orphaning server and ip resources",
            notes = "Find all VM id's that are failing destroy and potentially orphaning server and ip resources")
    public List<VmActionData> getAllFailedDestroys(
            @QueryParam("minimumAttempts") @DefaultValue("3") int minimumAttempts) {
        minimumAttempts = Math.max(1, minimumAttempts);
        List<Action> actions = vmActionService.getIncompleteActions(minimumAttempts, "DESTROY_VM");
        return mapActionsToVmActionData(actions);
    }

    @GET
    @Path("/incomplete/cancelaccount")
    @ApiOperation(value = "Find all VM id's that are failing to cancel their account",
            notes = "Find all VM id's that are failing to cancel their account")
    public List<VmActionData> getAllFailedCancelAccounts(
            @QueryParam("minimumAttempts") @DefaultValue("3") int minimumAttempts) {
        minimumAttempts = Math.max(1, minimumAttempts);
        List<Action> actions = vmActionService.getIncompleteActions(minimumAttempts, "CANCEL_ACCOUNT");
        return mapActionsToVmActionData(actions);
    }

    @GET
    @Path("/createsWithoutPanopta")
    @ApiOperation(value = "Find all VMs that were provisioned without Panopta",
            notes = "Find all VM create actions that completed without installing Panopta. A high percentage of " +
                    "these usually means Panopta is broken (their agent install is timing out, their yum repo is " +
                    "corrupt, etc).")
    public ActionTypeErrorData getCreatesWithoutPanopta(
            @QueryParam("windowSize") @DefaultValue("10") long windowSize) {
        List<Action> errors = vmActionService.getCreatesWithoutPanopta(windowSize);
        if (errors.isEmpty()) {
            return new ActionTypeErrorData(ActionType.CREATE_VM,
                                           0,
                                           0,
                                           true,
                                           errors);
        } else {
            Checkpoint checkpoint = monitorService.getCheckpoint(Checkpoint.Name.CREATES_WITHOUT_PANOPTA);
            Instant beginDate = checkpoint == null ? null : checkpoint.checkpoint;
            ActionListFilters actionFilters = new ActionListFilters();
            actionFilters.byStatus(ActionStatus.COMPLETE);
            actionFilters.byDateRange(beginDate, null);
            actionFilters.byType(ActionType.CREATE_VM);
            actionFilters.setLimit(windowSize);
            List<Long> allCreateIds = getFilteredActions(actionFilters)
                    .stream().map(a -> a.actionId).collect(Collectors.toList());
            errors.removeIf(e -> !allCreateIds.contains(e.id));
            return new ActionTypeErrorData(ActionType.CREATE_VM,
                                           ((double) errors.size() / allCreateIds.size()) * 100,
                                           getCountOfAffectedAccounts(errors),
                                           true,
                                           errors);
        }
    }

    @GET
    @Path("/replicationStatus")
    public ReplicationStatus getReplicationStatus() {
        Set<String> servers = databaseCluster.getServers();
        if (servers.isEmpty()) {
            throw new Vps4Exception("REPLICATION_CHECK_FAILED",
                                    "Replication check not configured for this environment");
        }

        String masterServer = popMasterServer(servers);
        ReplicationStatus replicationStatus = new ReplicationStatus(masterServer);

        String currentLocation = replicationLagService.getCurrentLocation(masterServer);
        for (String server : servers) {
            String lastReceiveLocation = replicationLagService.getLastReceiveLocation(server);
            double difference = replicationLagService.comparePgLsns(server, currentLocation, lastReceiveLocation);
            double lagInMegaBytes = Math.abs(difference / 1024 / 1024);
            ReplicationStatus.StandbyServer standbyServer = new ReplicationStatus.StandbyServer(server, lagInMegaBytes);
            replicationStatus.standbyServers.add(standbyServer);
        }

        return replicationStatus;
    }

    private String popMasterServer(Set<String> servers) {
        for (String server : servers) {
            if (replicationLagService.isMasterServer(server)) {
                servers.remove(server);
                return server;
            }
        }
        throw new Vps4Exception("REPLICATION_CHECK_FAILED", "No master server found in cluster");
    }
}
