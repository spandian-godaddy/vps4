package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.panopta.PanoptaAvailability;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaGraph;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.web.PaginatedResult;
import com.godaddy.vps4.web.vm.VmResource;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class VmMonitoringResourceTest {

    private VmMonitoringResource resource;
    private VmResource vmResource;
    private VirtualMachine vm;
    private List<PanoptaGraph> graphs;
    private UriInfo uriInfo;
    private PanoptaService panoptaService;
    private PanoptaDataService panoptaDataService;
    private VmOutageResource vmOutageResource;

    private final Injector injector = Guice.createInjector(new ObjectMapperModule());

    @Before
    public void setup() throws PanoptaServiceException {
        injector.injectMembers(this);
        vmResource = mock(VmResource.class);
        panoptaService = mock(PanoptaService.class);
        panoptaDataService = mock(PanoptaDataService.class);
        vmOutageResource = mock(VmOutageResource.class);
        resource = new VmMonitoringResource(vmResource, panoptaService, panoptaDataService, vmOutageResource);
        IpAddress ipAddress = new IpAddress(1, 0, null, null, null, null, null, 4);
        vm = new VirtualMachine(UUID.randomUUID(),
                                1L,
                                null,
                                1L,
                                null,
                                null,
                                null,
                                ipAddress,
                                Instant.now().minus(Duration.ofDays(5)),
                                null,
                                null,
                                null,
                                null,
                                0,
                                UUID.randomUUID(),
                                null);
        graphs = new ArrayList<>();
        graphs.add(createMockGraph(VmMetric.CPU));
        graphs.add(createMockGraph(VmMetric.DISK));
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        when(panoptaService.getUsageGraphs(vm.vmId, "week")).thenReturn(graphs);
        new JSONParser();
        setupUri();
    }

    private PanoptaGraph createMockGraph(VmMetric type) {
        PanoptaGraph graph = new PanoptaGraph();
        graph.type = type;
        graph.timestamps = new ArrayList<>();
        graph.timestamps.add(Instant.now().minus(2, ChronoUnit.HOURS));
        graph.timestamps.add(Instant.now().minus(1, ChronoUnit.HOURS));
        graph.values = new ArrayList<>();
        graph.values.add(Math.random());
        graph.values.add(Math.random());
        return graph;
    }

    private void setupUri(){
        uriInfo = mock(UriInfo.class);
        URI uri = null;
        try {
            uri = new URI("http://fakeUri/something/something/");
        } catch (URISyntaxException e) {
            // do nothing
        }
        when(uriInfo.getAbsolutePath()).thenReturn(uri);
    }

    @Test
    public void testGetVmUptimeForPanopta() throws PanoptaServiceException {
        PanoptaAvailability panoptaAvailability = new PanoptaAvailability();
        panoptaAvailability.availability = 0.9985360556398332;
        PanoptaDetail panoptaDetail = new PanoptaDetail(vm.vmId, "partnerCustomerKey",
                                                        "customerKey", 23, "serverKey",
                                                        Instant.now(), Instant.MAX);

        when(panoptaDataService.getPanoptaDetails(vm.vmId)).thenReturn(panoptaDetail);
        when(panoptaService.getAvailability(eq(vm.vmId), anyString(), anyString())).thenReturn(panoptaAvailability);

        List<MonitoringUptimeRecord> records = resource.getVmUptime(vm.vmId, 30);

        assertEquals(1, records.size());

        MonitoringUptimeRecord record = records.get(0);
        assertEquals(99.85360556398332, record.uptime, 0);
    }

    @Test
    public void testGetVmMonitoringEventsForPanopta() throws PanoptaServiceException {
        List<VmOutage> panoptaEvents = new ArrayList<>();
        Instant now = Instant.now();
        Instant lastMonth = Instant.now().minus(31, ChronoUnit.DAYS);

        VmOutage outage1 = new VmOutage();
        outage1.metrics = Collections.singleton(VmMetric.PING);
        outage1.started = now;
        outage1.ended = null;
        outage1.reason = "";
        panoptaEvents.add(outage1);
        VmOutage outage2 = new VmOutage();
        outage2.metrics = Collections.singleton(VmMetric.PING);
        outage2.started = lastMonth;
        outage2.ended = now;
        outage2.reason = "";
        panoptaEvents.add(outage2);

        PanoptaDetail panoptaDetail = new PanoptaDetail(vm.vmId, "partnerCustomerKey",
                                                        "customerKey", 3, "serverKey",
                                                        Instant.now(), Instant.MAX);
        when(panoptaDataService.getPanoptaDetails(vm.vmId)).thenReturn(panoptaDetail);
        when(vmOutageResource.getVmOutageList(vm.vmId, null, null, null)).thenReturn(panoptaEvents);

        PaginatedResult<MonitoringEvent> events = resource.getVmMonitoringEvents(vm.vmId, 30, 10, 0, uriInfo);

        assertEquals(1, events.results.size());
        assertEquals(1, events.pagination.total);
        MonitoringEvent event = events.results.get(0);
        assertEquals("outage", event.type);
        assertTrue(event.open);
        assertEquals(now, event.start);
        assertNull(event.end);
        assertEquals("", event.message);
    }

    @Test
    public void testGetMonitoringGraphs() throws PanoptaServiceException {
        String timescale = "randomScale" + (Math.random() * 100);

        when(vmResource.getVm(vm.vmId)).thenReturn(vm);

        resource.getMonitoringGraphs(vm.vmId, timescale, null);

        verify(vmResource).getVm(vm.vmId);
        verify(panoptaService).getUsageGraphs(vm.vmId, timescale);
        verify(panoptaService).getNetworkGraphs(vm.vmId, timescale);
    }

    @Test
    public void testGetMonitoringGraphsFilteredForUsage() throws PanoptaServiceException {
        String timescale = "randomScale" + (Math.random() * 100);

        when(vmResource.getVm(vm.vmId)).thenReturn(vm);

        resource.getMonitoringGraphs(vm.vmId, timescale, VmMonitoringResource.Category.USAGE);

        verify(vmResource).getVm(vm.vmId);
        verify(panoptaService).getUsageGraphs(vm.vmId, timescale);
        verify(panoptaService, never()).getNetworkGraphs(vm.vmId, timescale);
    }

    @Test
    public void testGetMonitoringGraphsFilteredForNetwork() throws PanoptaServiceException {
        String timescale = "randomScale" + (Math.random() * 100);

        when(vmResource.getVm(vm.vmId)).thenReturn(vm);

        resource.getMonitoringGraphs(vm.vmId, timescale, VmMonitoringResource.Category.NETWORK);

        verify(vmResource).getVm(vm.vmId);
        verify(panoptaService, never()).getUsageGraphs(vm.vmId, timescale);
        verify(panoptaService).getNetworkGraphs(vm.vmId, timescale);
    }

    @Test
    public void testGetMonitoringGraphsCsvCallsPanopta() throws PanoptaServiceException {
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);

        Response response = resource.getMonitoringGraphsCsv(vm.vmId, "week", null);

        verify(vmResource).getVm(vm.vmId);
        verify(panoptaService).getUsageGraphs(vm.vmId, "week");
        verify(panoptaService).getNetworkGraphs(vm.vmId, "week");
        assertEquals("attachment; filename=\"data.csv\"", response.getHeaderString("Content-Disposition"));
    }

    @Test
    public void testGetMonitoringGraphsCsvHeader() {
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);

        Response response = resource.getMonitoringGraphsCsv(vm.vmId, "week", null);
        ByteArrayInputStream resStream = (ByteArrayInputStream) response.getEntity();
        String[] resString = convertInputStreamToString(resStream).split("\n");
        String[] header = resString[0].trim().split(",");

        assertEquals("Timestamp", header[0]);
        assertEquals("CPU", header[1]);
        assertEquals("DISK", header[2]);
    }

    @Test
    public void testGetMonitoringGraphsCsvBody() {
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);

        Response response = resource.getMonitoringGraphsCsv(vm.vmId, "week", null);
        ByteArrayInputStream resStream = (ByteArrayInputStream) response.getEntity();
        String[] resString = convertInputStreamToString(resStream).split("\n");
        String[] line1 = resString[1].trim().split(",");
        String[] line2 = resString[2].trim().split(",");


        assertEquals(graphs.get(0).timestamps.get(0).toString(), line1[0]);
        assertEquals(graphs.get(0).values.get(0).toString(), line1[1]);
        assertEquals(graphs.get(1).values.get(0).toString(), line1[2]);
        assertEquals(graphs.get(0).timestamps.get(1).toString(), line2[0]);
        assertEquals(graphs.get(0).values.get(1).toString(), line2[1]);
        assertEquals(graphs.get(1).values.get(1).toString(), line2[2]);
    }

    private String convertInputStreamToString(InputStream stream) {
        try {
            int n = stream.available();
            byte[] bytes = new byte[ n ];
            stream.read(bytes, 0, n);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
