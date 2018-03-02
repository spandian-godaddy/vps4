package com.godaddy.vps4.web.monitoring;

import java.time.Duration;
import java.time.Instant;
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
import org.joda.time.LocalDate;

import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.PaginatedResult;
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

    private final NodePingService monitoringService;
    private final VmResource vmResource;
    private final MonitoringMeta monitoringMeta;

    @Inject
    public VmMonitoringResource(NodePingService monitoringService, VmResource vmResource, MonitoringMeta monitoringMeta) {
        this.monitoringService = monitoringService;
        this.vmResource = vmResource;
        this.monitoringMeta = monitoringMeta;
    }

    @GET
    @Path("/{vmId}/uptime")
    public List<MonitoringUptimeRecord> getVmUptime(@PathParam("vmId") UUID vmId, @QueryParam("days") @DefaultValue("30") Integer days) {
        VirtualMachine vm = vmResource.getVm(vmId);
        Instant start = Instant.now().minus(Duration.ofDays(days-1));
        if(start.isBefore(vm.validOn)){
            start = vm.validOn;
        }

        String startStr = start.toString();

        String end = LocalDate.now().plusDays(1).toString(); // the end date in the nodeping api is non-inclusive

        List<NodePingUptimeRecord> nodepingRecords = monitoringService.getCheckUptime(monitoringMeta.getAccountId(),
                vm.primaryIpAddress.pingCheckId,
                "days",
                startStr,
                end);

        sortRecordsByDateAsc(nodepingRecords);

        return nodepingRecords.stream().map(x -> new MonitoringUptimeRecord(x)).collect(Collectors.toList());
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

        nodepingRecords.sort(new Comparator<NodePingUptimeRecord>() {
            @Override
            public int compare(NodePingUptimeRecord o1, NodePingUptimeRecord o2) {
                return DateTime.parse(o1.label).compareTo(DateTime.parse(o2.label));
            }
        });
        // add the total record back because we still want to return it.
        nodepingRecords.add(total);
    }

    @GET
    @Path("/{vmId}/monitoringEvents")
    public PaginatedResult<MonitoringEvent> getVmMonitoringEvents(@PathParam("vmId") UUID vmId,
            @QueryParam("days") @DefaultValue("30") Integer days,
            @DefaultValue("10") @QueryParam("limit") Integer limit,
            @DefaultValue("0") @QueryParam("offset") Integer offset,
            @Context UriInfo uri) {
        VirtualMachine vm = vmResource.getVm(vmId);

        int scrubbedLimit = Math.max(limit, 0);
        int scrubbedOffset = Math.max(offset, 0);

        List<NodePingEvent> sourceEvents = monitoringService.getCheckEvents(monitoringMeta.getAccountId(),
                vm.primaryIpAddress.pingCheckId, 0);

        // only events from past "days" days
        List<MonitoringEvent> events = getDaysOfEvents(days, sourceEvents);

        int totalRows = events.size();

        // only the specified range of events.
        events = events.subList(Math.min(scrubbedOffset, events.size()), Math.min(scrubbedOffset+scrubbedLimit, events.size()));

        return new PaginatedResult<MonitoringEvent>(events, scrubbedLimit, scrubbedOffset, totalRows, uri);
    }

    private List<MonitoringEvent> getDaysOfEvents(Integer days,
            List<NodePingEvent> sourceEvents) {
        List<MonitoringEvent> events = sourceEvents.stream().map(x -> new MonitoringEvent(x)).collect(Collectors.toList());
        events = events.stream().filter(e -> e.start.isAfter(Instant.now().minus(Duration.ofDays(days))))
                .collect(Collectors.toList());
        return events;
    }


}
