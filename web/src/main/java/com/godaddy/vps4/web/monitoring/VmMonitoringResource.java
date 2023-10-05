package com.godaddy.vps4.web.monitoring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;

import com.godaddy.vps4.panopta.PanoptaAvailability;
import com.godaddy.vps4.panopta.PanoptaGraph;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.web.PaginatedResult;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmMonitoringResource {

    private final VmResource vmResource;
    private final PanoptaService panoptaService;

    @Inject
    public VmMonitoringResource(VmResource vmResource, PanoptaService panoptaService) {
        this.vmResource = vmResource;
        this.panoptaService = panoptaService;
    }

    @GET
    @Path("/{vmId}/uptime")
    @ApiOperation(value = "Get uptime data from monitoring service")
    public List<MonitoringUptimeRecord> getVmUptime(@PathParam("vmId") UUID vmId,
                                                    @QueryParam("days") @DefaultValue("30") Integer days) throws PanoptaServiceException {
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
        MonitoringUptimeRecord record = new MonitoringUptimeRecord("total", uptime.availability * 100);
        return Collections.singletonList(record);
    }

    @GET
    @Path("/{vmId}/monitoringEvents")
    public PaginatedResult<MonitoringEvent> getVmMonitoringEvents(
            @PathParam("vmId") UUID vmId,
            @QueryParam("days") @DefaultValue("30") Integer days,
            @DefaultValue("10") @QueryParam("limit") Integer limit,
            @DefaultValue("0") @QueryParam("offset") Integer offset,
            @Context UriInfo uri) throws PanoptaServiceException {
        vmResource.getVm(vmId); // auth validation

        int scrubbedLimit = Math.max(limit, 0);
        int scrubbedOffset = Math.max(offset, 0);
        List<MonitoringEvent> events;

        List<VmOutage> sourceEvents = panoptaService.getOutages(vmId, null, null, null);
        events = sourceEvents.stream()
                             .filter(event -> event.metrics.contains(VmMetric.PING))
                             .map(MonitoringEvent::new)
                             .collect(Collectors.toList());

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

    @GET
    @Produces("text/csv")
    @Path("/{vmId}/monitoringGraphsCsv")
    public Response getMonitoringGraphsCsv(@PathParam("vmId") UUID vmId,
            @ApiParam(value = "('hour', 'day', 'week', 'month', or 'year')", defaultValue = "hour", required = true)
            @QueryParam("timescale") String timescale,
            @QueryParam("type") Category category) {
        List<PanoptaGraph> graphs = getMonitoringGraphs(vmId, timescale, category);
        String[] headers = getCsvHeader(graphs);
        List<List<String>> csvBody = getCsvBody(graphs);
        ByteArrayInputStream response;
        try (
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out),
                                                       CSVFormat.Builder.create().setHeader(headers).build())
        ) {
            for (List<String> record : csvBody) {
                csvPrinter.printRecord(record);
            }
            csvPrinter.flush();
            response = new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new Vps4Exception("CSV_FAILED", "Failed to generate the CSV file");
        }
        return Response.ok(response)
                       .header("Content-Disposition", "attachment; filename=\"data.csv\"")
                       .build();
    }

    private String[] getCsvHeader(List<PanoptaGraph> graphs) {
        List<String> header = graphs.stream()
                                    .map(g -> g.type.toString())
                                    .collect(Collectors.toList());
        header.add(0, "Timestamp");
        return header.toArray(new String[0]);
    }

    private List<List<String>> getCsvBody(List<PanoptaGraph> graphs) {
        List<List<String>> body = new ArrayList<>();
        List<Instant> timestamps = graphs.get(0).timestamps;
        List<List<Double>> allValues = graphs.stream()
                                             .map(g -> g.values)
                                             .collect(Collectors.toList());
        for (int i = 0; i < timestamps.size(); i++) {
            List<String> record = new ArrayList<>();
            record.add(timestamps.get(i).toString());
            for (List<Double> values : allValues) {
                record.add((values.get(i) == null ? null : values.get(i).toString()));
            }
            body.add(record);
        }
        return body;
    }
}
