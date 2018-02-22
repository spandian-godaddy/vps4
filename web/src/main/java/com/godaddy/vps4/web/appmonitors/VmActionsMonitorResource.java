package com.godaddy.vps4.web.appmonitors;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.EmployeeOnly;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = { "appmonitors" })

@Path("/api/appmonitors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmActionsMonitorResource {

    private final MonitorService monitorService;

    @Inject
    public VmActionsMonitorResource(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @EmployeeOnly
    @GET
    @Path("/pending/provision")
    @ApiOperation(value = "Find all VM id's that are pending provisioning for longer than m minutes, default 60 minutes",
            notes = "Find all VM id's that are pending provisioning for longer than m minutes, default 60 minutes")
    public List<VmActionData> getProvisioningPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("60") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(thresholdInMinutes, ActionType.CREATE_VM, ActionStatus.IN_PROGRESS);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/startvm")
    @ApiOperation(value = "Find all VM id's that are pending start vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending start vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getStartPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        return monitorService.getVmsByActions( thresholdInMinutes, ActionType.START_VM, ActionStatus.IN_PROGRESS);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/stopvm")
    @ApiOperation(value = "Find all VM id's that are pending stop vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending stop vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getStopPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(thresholdInMinutes, ActionType.STOP_VM, ActionStatus.IN_PROGRESS);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/restartvm")
    @ApiOperation(value = "Find all VM id's that are pending restart vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending restart vm action for longer than m minutes, default 15 minutes")
    public List<VmActionData> getRestartPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(thresholdInMinutes, ActionType.RESTART_VM, ActionStatus.IN_PROGRESS);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/backupactions")
    @ApiOperation(value = "Find all snapshot ids that are pending backup vm action for longer than m minutes, default 2 hours",
            notes = "Find all snapshot id's that are pending backup vm action for longer than m minutes, default 2 hours")
    public List<SnapshotActionData> getBackupPendingActions(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsBySnapshotActions(thresholdInMinutes, ActionStatus.IN_PROGRESS, ActionStatus.NEW, ActionStatus.ERROR);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/restorevm")
    @ApiOperation(value = "Find all VM id's that are pending restore vm action for longer than m minutes, default 2 hours",
            notes = "Find all VM id's that are pending restore vm action for longer than m minutes, default 2 hours")
    public List<VmActionData> getRestorePendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(thresholdInMinutes, ActionType.RESTORE_VM, ActionStatus.IN_PROGRESS);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/newactions")
    @ApiOperation(value = "Find all vm actions pending in new status for longer than m minutes, default 2 hours",
            notes = "Find all VM actions that are pending in new status for longer than m minutes, default 2 hours")
    public List<VmActionData> getVmsWithPendingNewActions(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsByActionStatus(thresholdInMinutes, ActionStatus.NEW);
    }

}
