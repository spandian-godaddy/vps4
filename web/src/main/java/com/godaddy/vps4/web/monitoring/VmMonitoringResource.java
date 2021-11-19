package com.godaddy.vps4.web.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.joda.time.DateTime;

import com.godaddy.vps4.panopta.PanoptaAvailability;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaGraph;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.web.PaginatedResult;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import gdg.hfs.vhfs.nodeping.NodePingEvent;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.nodeping.NodePingUptimeRecord;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmMonitoringResource {

    private final NodePingService monitoringService;
    private final VmResource vmResource;
    private final MonitoringMeta monitoringMeta;
    private final PanoptaService panoptaService;
    private final PanoptaDataService panoptaDataService;
    private final VmOutageResource vmOutageResource;

    @Inject
    public VmMonitoringResource(NodePingService monitoringService,
                                VmResource vmResource,
                                MonitoringMeta monitoringMeta,
                                PanoptaService panoptaService,
                                PanoptaDataService panoptaDataService,
                                VmOutageResource vmOutageResource) {
        this.monitoringService = monitoringService;
        this.vmResource = vmResource;
        this.monitoringMeta = monitoringMeta;
        this.panoptaService = panoptaService;
        this.panoptaDataService = panoptaDataService;
        this.vmOutageResource = vmOutageResource;
    }

    @GET
    @Path("/{vmId}/uptime")
    @ApiOperation(value = "Get uptime data from Panopta or NodePing")
    public List<MonitoringUptimeRecord> getVmUptime(@PathParam("vmId") UUID vmId,
                                                    @QueryParam("days") @DefaultValue("30") Integer days) throws PanoptaServiceException {
        PanoptaDetail detail = panoptaDataService.getPanoptaDetails(vmId);
        return (detail != null) ? getPanoptaUptime(vmId, days) : getNodePingUptime(vmId, days);
    }

    private List<MonitoringUptimeRecord> getPanoptaUptime(UUID vmId, Integer days) throws PanoptaServiceException {
        VirtualMachine vm = vmResource.getVm(vmId);
        Instant start = Instant.now().minus(Duration.ofDays(days));
        Instant end = Instant.now();
        if (start.isBefore(vm.validOn)) {
            start = vm.validOn;
        }
        DateTimeFormatter dtf = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneOffset.UTC);
        PanoptaAvailability uptime = panoptaService.getAvailability(vmId,
                                                                    dtf.format(start),
                                                                    dtf.format(end));
        NodePingUptimeRecord record = new NodePingUptimeRecord();
        record.label = "total";
        record.uptime = uptime.availability * 100;
        return Collections.singletonList(new MonitoringUptimeRecord(record));
    }

    private List<MonitoringUptimeRecord> getNodePingUptime(UUID vmId, Integer days) {
        VirtualMachine vm = vmResource.getVm(vmId);
        Instant start = Instant.now().minus(Duration.ofDays(days - 1));
        Instant end = Instant.now().plus(Duration.ofDays(1)); // NodePing end date is non-inclusive
        if (start.isBefore(vm.validOn)) {
            start = vm.validOn;
        }
        List<NodePingUptimeRecord> nodepingRecords = monitoringService.getCheckUptime(monitoringMeta.getAccountId(),
                                                                                      vm.primaryIpAddress.pingCheckId,
                                                                                      "days",
                                                                                      start.toString(),
                                                                                      end.toString());
        sortRecordsByDateAsc(nodepingRecords);
        return nodepingRecords.stream().map(MonitoringUptimeRecord::new).collect(Collectors.toList());
    }

    private void sortRecordsByDateAsc(List<NodePingUptimeRecord> nodepingRecords) {
        NodePingUptimeRecord total = null;
        // remove the "total" record so it doesn't blow up the date parsing in the sort.
        for (NodePingUptimeRecord record : nodepingRecords){
            if(record.label.equals("total")){
                total = record;
                nodepingRecords.remove(record);
                break;
            }
        }

        nodepingRecords.sort(Comparator.comparing(o -> DateTime.parse(o.label)));
        // add the total record back because we still want to return it.
        nodepingRecords.add(total);
    }

    @GET
    @Path("/{vmId}/monitoringEvents")
    public PaginatedResult<MonitoringEvent> getVmMonitoringEvents(
            @PathParam("vmId") UUID vmId,
            @QueryParam("days") @DefaultValue("30") Integer days,
            @DefaultValue("10") @QueryParam("limit") Integer limit,
            @DefaultValue("0") @QueryParam("offset") Integer offset,
            @Context UriInfo uri) throws PanoptaServiceException {
        VirtualMachine vm = vmResource.getVm(vmId);
        int scrubbedLimit = Math.max(limit, 0);
        int scrubbedOffset = Math.max(offset, 0);
        List<MonitoringEvent> events;

        if (panoptaDataService.getPanoptaDetails(vmId) != null) {
            List<VmOutage> sourceEvents = vmOutageResource.getVmOutageList(vmId, false);
            events = sourceEvents.stream()
                                 .filter(event -> event.metrics.contains(VmMetric.PING))
                                 .map(MonitoringEvent::new)
                                 .collect(Collectors.toList());
        } else {
            List<NodePingEvent> sourceEvents = monitoringService
                    .getCheckEvents(monitoringMeta.getAccountId(), vm.primaryIpAddress.pingCheckId, 0);
            events = sourceEvents.stream()
                                 .map(MonitoringEvent::new)
                                 .collect(Collectors.toList());
        }

        // only events from past "days" days
        events = events.stream()
                       .filter(e -> e.start.isAfter(Instant.now().minus(Duration.ofDays(days))))
                       .collect(Collectors.toList());

        // only the specified page of events
        List<MonitoringEvent> paginatedEvents = events.subList(Math.min(scrubbedOffset, events.size()),
                                Math.min(scrubbedOffset + scrubbedLimit, events.size()));

        return new PaginatedResult<>(paginatedEvents, scrubbedLimit, scrubbedOffset, events.size(), uri);
    }

    public enum Category {USAGE, NETWORK}

    @GET
    @Path("/{vmId}/monitoringGraphs")
    public List<PanoptaGraph> getMonitoringGraphs(@PathParam("vmId") UUID vmId,
              @ApiParam(value = "('hour', 'day', 'week', 'month', or 'year')", defaultValue = "hour", required = true)
              @QueryParam("timescale") String timescale,
              @QueryParam("type") Category category) {
        vmResource.getVm(vmId);
        try {
            if (category == Category.USAGE) {
                return panoptaService.getUsageGraphs(vmId, timescale);
            }
            if (category == Category.NETWORK) {
                return panoptaService.getNetworkGraphs(vmId, timescale);
            }
            List<PanoptaGraph> graphs = new ArrayList<>();
            graphs.addAll(panoptaService.getUsageGraphs(vmId, timescale));
            graphs.addAll(panoptaService.getNetworkGraphs(vmId, timescale));
            return graphs;
        } catch (PanoptaServiceException e) {
            throw new Vps4Exception(e.getId(), e.getMessage(), e);
        }
    }
}
