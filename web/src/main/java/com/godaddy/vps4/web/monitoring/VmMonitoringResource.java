package com.godaddy.vps4.web.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import gdg.hfs.vhfs.nodeping.NodePingEvent;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.nodeping.NodePingUptimeRecord;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmMonitoringResource {

    private static final Logger logger = LoggerFactory.getLogger(VmMonitoringResource.class);

    private final NodePingService monitoringService;
    private final long monitoringAccountId;
    private final VmResource vmResource;
    private final Config config;
    private final int DaysOfMonitoring = 30;

    @Inject
    public VmMonitoringResource(NodePingService monitoringService, VmResource vmResource,
            Config config) {
        this.monitoringService = monitoringService;
        this.vmResource = vmResource;
        this.config = config;
        monitoringAccountId = Long.parseLong(this.config.get("nodeping.accountid"));
    }

    @GET
    @Path("/{vmId}/uptime")
    public List<MonitoringUptimeRecord> getVmUptime(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        
        List<NodePingUptimeRecord> nodepingRecords = monitoringService.getCheckUptime(monitoringAccountId,
                vm.primaryIpAddress.pingCheckId,
                "days",
                LocalDate.now().minusDays(DaysOfMonitoring).toString(),
                LocalDate.now().toString());

        return nodepingRecords.stream().map(x -> new MonitoringUptimeRecord(x)).collect(Collectors.toList());
    }

    @GET
    @Path("/{vmId}/monitoringEvents")
    public List<MonitoringEvent> getVmMonitoringEvents(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);

        List<NodePingEvent> sourceEvents = monitoringService.getCheckEvents(monitoringAccountId,
                vm.primaryIpAddress.pingCheckId, 0);
        List<MonitoringEvent> events = sourceEvents.stream().map(x -> new MonitoringEvent(x)).collect(Collectors.toList());
        events = events.stream().filter(e -> e.start.isAfter(Instant.now().minus(Duration.ofDays(DaysOfMonitoring))))
                .collect(Collectors.toList());
        return events;
    }
}
