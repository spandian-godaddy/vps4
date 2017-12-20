package com.godaddy.vps4.web.monitors;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.monitors.MonitorService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.AdminOnly;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Vps4Api
@Api(tags = { "monitors" })

@Path("/api/monitors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProvisioningMonitorResource {

    private static final Logger logger = LoggerFactory.getLogger(ProvisioningMonitorResource.class);

    private final MonitorService monitorService;

    @Inject
    public ProvisioningMonitorResource(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @AdminOnly
    @GET
    @Path("/provisioning/pending")
    @ApiOperation(value = "Find all VM id's that are pending provisioning for longer than m minutes, default 60 minutes",
            notes = "Find all VM id's that are pending provisioning for longer than m minutes, default 60 minutes")
    public List<UUID> getProvisioningPendingVms(@QueryParam("thresholdInMinutes") @DefaultValue("60") Long thresholdInMinutes) {
        return monitorService.getVmsByActions(ActionType.CREATE_VM, ActionStatus.IN_PROGRESS, thresholdInMinutes);
    }

}
