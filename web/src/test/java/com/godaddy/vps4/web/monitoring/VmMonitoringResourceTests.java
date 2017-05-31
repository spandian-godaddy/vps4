package com.godaddy.vps4.web.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.PaginatedResult;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.vhfs.nodeping.NodePingEvent;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.nodeping.NodePingUptimeRecord;

public class VmMonitoringResourceTests {

    VmMonitoringResource resource;
    VmResource vmResource;
    Config config;
    NodePingService monitoringService;
    VirtualMachine vm;
    long monitoringAccountId = 12;
    JSONParser parser;
    UriInfo uriInfo;



    @Before
    public void setup() {
        monitoringService = mock(NodePingService.class);
        vmResource = mock(VmResource.class);
        config = mock(Config.class);
        IpAddress ipAddress = new IpAddress(0, null, null, null, 123L, null, null);
        vm = new VirtualMachine(UUID.randomUUID(), 1L, null, 1L, null, null, null, ipAddress, Instant.now().minus(Duration.ofDays(5)), null, null, null);
        when(vmResource.getVm(vm.vmId)).thenReturn(vm);
        when(config.get("nodeping.accountid")).thenReturn("12");
        parser = new JSONParser();
        setupUri();
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
    public void testGetVmUptime() throws ParseException {
        List<NodePingUptimeRecord> npRecords = new ArrayList<>();
        JSONObject jsonObject = (JSONObject) parser.parse("{\"enabled\": 2678400000,\"down\": 35210534,\"uptime\": 98.685}");
        JSONObject jsonObject2 = (JSONObject) parser.parse("{\"enabled\": 3789511111,\"down\": 35210645,\"uptime\": 95.685}");
        npRecords.add(new NodePingUptimeRecord("2017-05-05", jsonObject));
        npRecords.add(new NodePingUptimeRecord("2017-05-06", jsonObject2));
        npRecords.add(new NodePingUptimeRecord("total", jsonObject2));


        when(monitoringService.getCheckUptime(eq(monitoringAccountId), eq(vm.primaryIpAddress.pingCheckId), eq("days"), anyString(),
                anyString()))
                .thenReturn(npRecords);

        resource = new VmMonitoringResource(monitoringService, vmResource, config);
        List<MonitoringUptimeRecord> records = resource.getVmUptime(vm.vmId, 30);

        assertEquals(3, records.size());

        MonitoringUptimeRecord first = records.stream().filter(x -> x.label == "2017-05-05").findFirst().get();
        assertEquals(98.685, first.uptime, 0);

        MonitoringUptimeRecord second = records.stream().filter(x -> x.label == "2017-05-06").findFirst().get();
        assertEquals(95.685, second.uptime, 0);
    }

    @Test
    public void testGetVmMonitoringEvents() throws ParseException {
        List<NodePingEvent> npEvents = new ArrayList<>();
        long now = Instant.now().toEpochMilli();
        long lastMonth = Instant.now().minus(31, ChronoUnit.DAYS).toEpochMilli();

        JSONObject jsonObject = (JSONObject) parser
                .parse(String.format("{\"type\": \"down\",\"start\":%d,\"end\":%d,\"open\":true,\"message\":\"timeout\"}", now, null));
        JSONObject jsonObject2 = (JSONObject) parser
                .parse(String.format("{\"type\": \"down\",\"start\":%d,\"end\":%d,\"open\":false,\"message\":\"timeout\"}", lastMonth,
                        now));
        npEvents.add(new NodePingEvent(jsonObject));
        npEvents.add(new NodePingEvent(jsonObject2));

        when(monitoringService.getCheckEvents(monitoringAccountId, vm.primaryIpAddress.pingCheckId, 0)).thenReturn(npEvents);

        resource = new VmMonitoringResource(monitoringService, vmResource, config);
        PaginatedResult<MonitoringEvent> events = resource.getVmMonitoringEvents(vm.vmId, Integer.valueOf(30),
                Integer.valueOf(10), Integer.valueOf(0), uriInfo);

        assertEquals(1, events.results.size());
        assertEquals(1, events.pagination.total);
        MonitoringEvent event = events.results.get(0);
        assertEquals("down", event.type);
        assertTrue(event.open);
        assertEquals(Instant.ofEpochMilli(now), event.start);
        assertNull(event.end);
        assertEquals("timeout", event.message);
    }

}
