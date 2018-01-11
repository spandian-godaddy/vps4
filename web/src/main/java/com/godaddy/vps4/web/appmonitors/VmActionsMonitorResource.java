package com.godaddy.vps4.web.appmonitors;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.EmployeeOnly;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vps4Api
@Api(tags = { "appmonitors" })

@Path("/api/appmonitors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmActionsMonitorResource {

    private static final Logger logger = LoggerFactory.getLogger(VmActionsMonitorResource.class);

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
    public List<UUID> getProvisioningPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("60") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, thresholdInMinutes);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/startvm")
    @ApiOperation(value = "Find all VM id's that are pending start vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending start vm action for longer than m minutes, default 15 minutes")
    public List<UUID> getStartPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(ActionType.START_VM, ActionStatus.IN_PROGRESS, thresholdInMinutes);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/stopvm")
    @ApiOperation(value = "Find all VM id's that are pending stop vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending stop vm action for longer than m minutes, default 15 minutes")
    public List<UUID> getStopPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(ActionType.STOP_VM, ActionStatus.IN_PROGRESS, thresholdInMinutes);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/restartvm")
    @ApiOperation(value = "Find all VM id's that are pending restart vm action for longer than m minutes, default 15 minutes",
            notes = "Find all VM id's that are pending restart vm action for longer than m minutes, default 15 minutes")
    public List<UUID> getRestartPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("15") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(ActionType.RESTART_VM, ActionStatus.IN_PROGRESS, thresholdInMinutes);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/backupvm")
    @ApiOperation(value = "Find all VM id's that are pending backup vm action for longer than m minutes, default 2 hours",
            notes = "Find all VM id's that are pending backup vm action for longer than m minutes, default 2 hours")
    public List<UUID> getBackupPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(ActionType.CREATE_SNAPSHOT, ActionStatus.IN_PROGRESS, thresholdInMinutes);
    }

    @EmployeeOnly
    @GET
    @Path("/pending/restorevm")
    @ApiOperation(value = "Find all VM id's that are pending restore vm action for longer than m minutes, default 2 hours",
            notes = "Find all VM id's that are pending restore vm action for longer than m minutes, default 2 hours")
    public List<UUID> getRestorePendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("120") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(ActionType.RESTORE_VM, ActionStatus.IN_PROGRESS, thresholdInMinutes);
    }

}
