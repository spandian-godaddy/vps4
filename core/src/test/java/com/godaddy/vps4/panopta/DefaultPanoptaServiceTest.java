package com.godaddy.vps4.panopta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.NotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.panopta.jdbc.PanoptaCustomerDetails;
import com.godaddy.vps4.util.ObjectMapperProvider;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.google.inject.Inject;

@RunWith(MockitoJUnitRunner.class)
public class DefaultPanoptaServiceTest {
    @Mock private CacheManager cacheManager;
    @Mock private PanoptaApiServerService panoptaApiServerService;
    @Mock private PanoptaApiCustomerService panoptaApiCustomerService;
    @Mock private PanoptaApiServerGroupService panoptaApiServerGroupService;
    @Mock private PanoptaApiOutageService panoptaApiOutageService;
    @Mock private PanoptaDataService panoptaDataService;
    @Mock private VirtualMachineService virtualMachineService;
    @Mock private CreditService creditService;
    @Mock private Config config;
    @Mock private Cache cache;
    @Mock private PanoptaMetricMapper panoptaMetricMapper;
    @Mock private PanoptaCustomerDetails panoptaCustomerDetails;
    @Mock private PanoptaApiCustomerList panoptaApiCustomerList;

    @Captor private ArgumentCaptor<PanoptaApiAttributeRequest> attributeRequest;
    @Captor private ArgumentCaptor<PanoptaApiUpdateServerRequest> updateServerRequest;

    private @PanoptaExecutorService ExecutorService pool;
    private UUID vmId;
    private UUID orionGuid;
    private String ipAddress;
    private VirtualMachineCredit credit;
    private VirtualMachine virtualMachine;
    private long serverId;
    private String shopperId;
    private String serverKey;
    private String customerKey;
    private String partnerCustomerKey;
    private DefaultPanoptaService defaultPanoptaService;
    private PanoptaDetail panoptaDetail;
    private PanoptaServers panoptaServers;
    private PanoptaServers.Server server;
    private PanoptaUsageIdList usageIdList;
    private PanoptaNetworkIdList networkIdList;
    private PanoptaUsageGraph usageGraph;
    private PanoptaNetworkGraph networkGraph;

    @Inject private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Before
    public void setup() throws IOException {
        pool = spy(Executors.newCachedThreadPool());
        vmId = UUID.randomUUID();
        orionGuid = UUID.randomUUID();
        ipAddress = "127.0.0.1";
        virtualMachine = new VirtualMachine();
        virtualMachine.primaryIpAddress = new IpAddress();
        virtualMachine.primaryIpAddress.ipAddress = ipAddress;
        virtualMachine.orionGuid = orionGuid;
        virtualMachine.vmId = vmId;
        serverId = 42;
        shopperId = "dummy-shopper-id";
        credit = createDummyCredit();
        partnerCustomerKey = "gdtest_" + shopperId;
        customerKey = "someCustomerKey";
        serverKey = "someServerKey";
        panoptaDetail = new PanoptaDetail(vmId, partnerCustomerKey,
                                          customerKey, serverId, serverKey,
                                          Instant.now(), Instant.MAX);
        setupPanoptaServers();
        setupGraphIdLists();
        setupGraphs();
        setupCustomerList();
        setupServerGroups();
        setupOtherMocks();
        when(cacheManager.getCache(CacheName.PANOPTA_METRIC_GRAPH,
                                   String.class,
                                   DefaultPanoptaService.CachedMonitoringGraphs.class)).thenReturn(cache);
        defaultPanoptaService = new DefaultPanoptaService(pool,
                                                          cacheManager,
                                                          panoptaApiCustomerService,
                                                          panoptaApiServerService,
                                                          panoptaApiServerGroupService,
                                                          panoptaApiOutageService,
                                                          panoptaDataService,
                                                          config,
                                                          panoptaMetricMapper);
    }

    @After
    public void teardown() {
        pool.shutdown();
    }

    private void setupPanoptaServers() {
        panoptaServers = new PanoptaServers();
        panoptaServers.servers = new ArrayList<>();
        server = mock(PanoptaServers.Server.class);
        server.fqdn = ipAddress;
        server.serverKey = serverKey;
        server.name = virtualMachine.orionGuid.toString();
        server.url = "https://api2.panopta.com/v2/server/" + serverId;
        server.status = PanoptaServer.Status.ACTIVE.toString();
        server.agentLastSynced = "2021-01-19 17:22:20";
        server.additionalFqdns = Arrays.asList("definitelyFakeFQDN.here", "definitelyFakeFQDN2.here");
        panoptaServers.servers.add(server);
    }

    private void setupGraphIdLists() {
        usageIdList = new PanoptaUsageIdList();
        usageIdList.value = Collections.singletonList(createDummyMetricId(null));
        networkIdList = new PanoptaNetworkIdList();
        networkIdList.value = new ArrayList<>();
        networkIdList.value.add(createDummyMetricId("additionalFqdn.fake"));
        networkIdList.value.add(createDummyMetricId("additionalFqdn2.fake"));
    }

    private PanoptaMetricId createDummyMetricId(String serverInterface) {
        PanoptaMetricId metricId = new PanoptaMetricId();
        metricId.id = (long) (Math.random() * 9999);
        metricId.typeId = (long) (Math.random() * 9999);
        metricId.serverInterface = serverInterface;
        metricId.status = "active";
        return metricId;
    }

    private void setupGraphs() {
        this.usageGraph = new PanoptaUsageGraph();
        usageGraph.type = VmMetric.SSH;
        usageGraph.timestamps = new ArrayList<>();
        usageGraph.values = new ArrayList<>();
        this.networkGraph = new PanoptaNetworkGraph();
        networkGraph.type = VmMetric.FTP;
        networkGraph.timestamps = new ArrayList<>();
        networkGraph.values = new ArrayList<>();
    }

    private void setupCustomerList() throws IOException {
        String mock = "{\n" +
                "  \"customer_list\": [\n" +
                "    {\n" +
                "      \"customer_key\": \"2hum-wpmt-vswt-2g3b\",\n" +
                "      \"email_address\": \"abhoite@godaddy.com\",\n" +
                "      \"name\": \"Godaddy VPS4 POC\",\n" +
                "      \"package\": \"godaddy.fully_managed\",\n" +
                "      \"partner_customer_key\": \"" + partnerCustomerKey + "\",\n" +
                "      \"status\": \"active\",\n" +
                "      \"url\": \"https://api2.panopta.com/v2/customer/2hum-wpmt-vswt-2g3b\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"meta\": {\n" +
                "    \"limit\": 50,\n" +
                "    \"next\": null,\n" +
                "    \"offset\": 0,\n" +
                "    \"previous\": null,\n" +
                "    \"total_count\": 1\n" +
                "  }\n" +
                "}\n";
        PanoptaApiCustomerList fakePanoptaCustomers = objectMapper.readValue(mock, PanoptaApiCustomerList.class);
        when(panoptaApiCustomerService.getCustomer(partnerCustomerKey)).thenReturn(fakePanoptaCustomers);
        when(panoptaApiCustomerList.getCustomerList()).thenReturn(fakePanoptaCustomers.getCustomerList());
    }

    private void setupServerGroups() throws IOException {
        String mock = "{\n" +
                "  \"server_group_list\": [\n" +
                "    {\n" +
                "      \"name\": \"Default Server Group\",\n" +
                "      \"notification_schedule\": \"https://api2.panopta.com/v2/notification_schedule/224324\",\n" +
                "      \"server_group\": null,\n" +
                "      \"tags\": [],\n" +
                "      \"url\": \"https://api2.panopta.com/v2/server_group/428250\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        PanoptaServerGroupList fakeGroups = objectMapper.readValue(mock, PanoptaServerGroupList.class);
        when(panoptaApiServerGroupService.getServerGroups(partnerCustomerKey)).thenReturn(fakeGroups);
    }

    private void setupOtherMocks() {
        when(virtualMachineService.getVirtualMachine(any(UUID.class))).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(any(UUID.class))).thenReturn(credit);
        when(config.get("panopta.api.part" +
                                "ner.customer.key.prefix")).thenReturn("gdtest_");
        doNothing().when(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));
        when(panoptaApiServerService.getUsageList(serverId, partnerCustomerKey, 0)).thenReturn(usageIdList);
        when(panoptaApiServerService.getNetworkList(serverId, partnerCustomerKey, 0)).thenReturn(networkIdList);
        when(panoptaApiServerService.getServer(serverId, partnerCustomerKey)).thenReturn(server);
        when(panoptaApiServerService.getServers(partnerCustomerKey, ipAddress, orionGuid.toString(), "active"))
                .thenReturn(panoptaServers);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
    }

    private VirtualMachineCredit createDummyCredit() {
        return new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withShopperID(shopperId)
                .build();
    }

    @Test(expected = PanoptaServiceException.class)
    public void testInvokesCreatesCustomer() throws PanoptaServiceException {
        when(panoptaApiCustomerService.getCustomer("gdtest_nonexistent"))
                .thenReturn(panoptaApiCustomerList);

        defaultPanoptaService.createCustomer("nonexistent");

        verify(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));
    }

    @Test
    public void testInvokesCreatesCustomerWithMatchingCustomer() throws PanoptaServiceException {
        defaultPanoptaService.createCustomer(shopperId);
        verify(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));
    }

    @Test
    public void testSetAttributes() {
        Map<Long, String> attributes = new HashMap<>();
        attributes.put(1234L, "mock-data-1");
        attributes.put(5678L, "mock-data-2");
        defaultPanoptaService.setServerAttributes(vmId, attributes);
        verify(panoptaApiServerService, times(2))
                .setAttribute(eq(serverId), eq(partnerCustomerKey), attributeRequest.capture());

        List<PanoptaApiAttributeRequest> requests = attributeRequest.getAllValues();
        assertTrue(requests.stream().anyMatch(r -> r.getValue().equals("mock-data-1")
                && r.getServerAttributeTypeUrl().equals("https://api2.panopta.com/v2/server_attribute_type/1234")));
        assertTrue(requests.stream().anyMatch(r -> r.getValue().equals("mock-data-2")
                && r.getServerAttributeTypeUrl().equals("https://api2.panopta.com/v2/server_attribute_type/5678")));
    }

    @Test
    public void testGetUsageIdsWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.getUsageIds(vmId);
        verify(panoptaApiServerService, never()).getUsageList(anyInt(), anyString(), anyInt());
    }

    @Test
    public void testGetUsageIds() {
        List<PanoptaMetricId> ids = defaultPanoptaService.getUsageIds(vmId);
        verify(panoptaApiServerService).getUsageList(serverId, partnerCustomerKey, 0);
        assertEquals(ids.size(), 1);
        assertEquals(ids.get(0).id, usageIdList.value.get(0).id);
        assertEquals(ids.get(0).typeId, usageIdList.value.get(0).typeId);
    }

    @Test
    public void testGetNetworkIdsWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.getNetworkIds(vmId);
        verify(panoptaApiServerService, never()).getNetworkList(anyInt(), anyString(), anyInt());
    }

    @Test
    public void testGetNetworkIds() {
        List<PanoptaMetricId> ids = defaultPanoptaService.getNetworkIds(vmId);
        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey, 0);
        assertEquals(2, ids.size());
        assertEquals(networkIdList.value.get(0).id, ids.get(0).id);
        assertEquals(networkIdList.value.get(0).typeId, ids.get(0).typeId);
    }

    @Test
    public void testGetNetworkIdOfAdditionalFqdn() throws PanoptaServiceException {
        PanoptaMetricId affectedMetric = networkIdList.value.get(0);
        when(panoptaMetricMapper.getVmMetric(affectedMetric.typeId)).thenReturn(VmMetric.HTTPS);
        PanoptaMetricId ids = defaultPanoptaService.getNetworkIdOfAdditionalFqdn(vmId, "additionalFqdn.fake");
        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey, 0);
        assertEquals(networkIdList.value.get(0).id, ids.id);
        assertEquals(networkIdList.value.get(0).typeId, ids.typeId);
    }

    @Test
    public void testGetAdditionalFqdnMetricIds() {
        Map<String, Instant> fqdnValidOnMap = new HashMap<>();
        fqdnValidOnMap.put("additionalFqdn.fake", Instant.now());
        fqdnValidOnMap.put("additionalFqdn2.fake", Instant.now());
        when(panoptaMetricMapper.getVmMetric(anyLong())).thenReturn(VmMetric.HTTPS);
        when(panoptaDataService.getPanoptaAdditionalFqdnWithValidOn(vmId)).thenReturn(fqdnValidOnMap);
        List<PanoptaDomain> domains = defaultPanoptaService.getAdditionalDomains(vmId);
        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey, 0);
        assertEquals(2, domains.size());
        assertEquals(networkIdList.value.get(0).id, domains.get(0).id);
        assertEquals(networkIdList.value.get(0).typeId, domains.get(0).typeId);
    }

    @Test
    public void testGetUsageGraphs() throws PanoptaServiceException, InterruptedException {
        when(cache.containsKey(String.format("%s.usage.%s", vmId, "hour"))).thenReturn(false);
        when(panoptaApiServerService.getUsageGraph(eq(serverId),
                                                   anyInt(),
                                                   eq("hour"),
                                                   eq(partnerCustomerKey))).thenReturn(usageGraph);

        List<PanoptaGraph> graphs = defaultPanoptaService.getUsageGraphs(vmId, "hour");

        verify(panoptaApiServerService).getUsageList(serverId, partnerCustomerKey, 0);
        verify(panoptaApiServerService)
                .getUsageGraph(eq(serverId), anyInt(), eq("hour"), eq(partnerCustomerKey));
        verify(pool, times(1))
                .invokeAll(any(Collection.class), eq(1L), eq(TimeUnit.MINUTES));
        assertEquals(graphs.get(0), usageGraph);
    }

    @Test
    public void testGetNetworkGraphs() throws PanoptaServiceException, InterruptedException {
        when(cache.containsKey(String.format("%s.network.%s", vmId, "hour"))).thenReturn(false);
        when(panoptaApiServerService.getNetworkGraph(eq(serverId),
                                                     anyInt(),
                                                     eq("hour"),
                                                     eq(partnerCustomerKey))).thenReturn(networkGraph);

        List<PanoptaGraph> graphs = defaultPanoptaService.getNetworkGraphs(vmId, "hour");

        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey,0);
        verify(panoptaApiServerService, times(2))
                .getNetworkGraph(eq(serverId), anyInt(), eq("hour"), eq(partnerCustomerKey));
        verify(pool, times(1))
                .invokeAll(any(Collection.class), eq(1L), eq(TimeUnit.MINUTES));
        assertEquals(graphs.get(0), networkGraph);
    }

    @Test
    public void testGetUsageGraphsWithCache() throws PanoptaServiceException {
        List<PanoptaGraph> panoptaGraphs = new ArrayList<>();
        panoptaGraphs.add(usageGraph);
        DefaultPanoptaService.CachedMonitoringGraphs
                cachedGraphs = new DefaultPanoptaService.CachedMonitoringGraphs(panoptaGraphs);

        when(cache.containsKey(String.format("%s.usage.%s", vmId, "hour"))).thenReturn(true);
        when(cache.containsKey(String.format("%s.usage.%s", vmId, "hour"))).thenReturn(true);
        when(cache.get(String.format("%s.usage.%s", vmId, "hour"))).thenReturn(cachedGraphs);

        List<PanoptaGraph> graphs = defaultPanoptaService.getUsageGraphs(vmId, "hour");

        verify(panoptaApiServerService, never()).getUsageList(anyInt(), anyString(), anyInt());
        verify(panoptaApiServerService, never())
                .getUsageGraph(anyInt(), anyInt(), anyString(), anyString());
        assertEquals(graphs.get(0), usageGraph);
    }

    @Test
    public void testGetNetworkGraphsWithCache() throws PanoptaServiceException {
        List<PanoptaGraph> panoptaGraphs = new ArrayList<>();
        panoptaGraphs.add(networkGraph);
        DefaultPanoptaService.CachedMonitoringGraphs
                cachedGraphs = new DefaultPanoptaService.CachedMonitoringGraphs(panoptaGraphs);

        when(cache.containsKey(String.format("%s.network.%s", vmId, "hour"))).thenReturn(true);
        when(cache.get(String.format("%s.network.%s", vmId, "hour"))).thenReturn(cachedGraphs);

        List<PanoptaGraph> graphs = defaultPanoptaService.getNetworkGraphs(vmId, "hour");

        verify(panoptaApiServerService, never()).getNetworkList(anyInt(), anyString(), anyInt());
        verify(panoptaApiServerService, never())
                .getNetworkGraph(anyInt(), anyInt(), anyString(), anyString());
        assertEquals(graphs.get(0), networkGraph);
    }

    @Test(expected = PanoptaServiceException.class)
    public void testGetUsageGraphsWhenNoDbEntry() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.getUsageGraphs(vmId, "hour");
    }

    @Test(expected = PanoptaServiceException.class)
    public void testGetNetworkGraphsWhenNoDbEntry() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.getNetworkGraphs(vmId, "hour");
    }

    @Test
    public void testDoesNotPauseMonitoringWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.pauseServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).getServer(serverId, partnerCustomerKey);
    }

    @Test
    public void testDoesNotPauseMonitoringWhenPanoptaAlreadySuspended() {
        server.status = "suspended";
        defaultPanoptaService.pauseServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).updateServer(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testPauseMonitoringSuccess() {
        server.status = "active";
        defaultPanoptaService.pauseServerMonitoring(vmId);
        verify(panoptaApiServerService).updateServer(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testDoesNotResumeMonitoringWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.resumeServerMonitoring(vmId, orionGuid);
        verify(panoptaApiServerService, never()).getServer(serverId, partnerCustomerKey);
    }

    @Test
    public void testDoesNotResumeMonitoringWhenPanoptaAlreadyActive() {
        server.status = "active";
        defaultPanoptaService.resumeServerMonitoring(vmId, orionGuid);
        verify(panoptaApiServerService, never()).updateServer(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testResumeMonitoringSuccess() {
        server.status = "suspended";
        defaultPanoptaService.resumeServerMonitoring(vmId, orionGuid);
        verify(panoptaApiServerService).updateServer(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testDeleteCustomerCallsPanoptaApi() {
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(panoptaCustomerDetails);
        when(panoptaCustomerDetails.getCustomerKey()).thenReturn(customerKey);
        defaultPanoptaService.deleteCustomer(shopperId);
        verify(panoptaApiCustomerService).deleteCustomer(customerKey);
    }

    @Test
    public void testCreateServer() throws PanoptaServiceException {
        String ipAddress = virtualMachine.primaryIpAddress.ipAddress;
        PanoptaServer server = defaultPanoptaService.createServer(shopperId, orionGuid, ipAddress, null, null);
        verify(panoptaApiServerService).createServer(eq(partnerCustomerKey), any(PanoptaApiServerRequest.class));
        verify(panoptaApiServerService).getServers(partnerCustomerKey, ipAddress, orionGuid.toString(), "active");
        assertEquals(panoptaServers.servers.get(0).fqdn, server.fqdn);
        assertEquals(panoptaServers.servers.get(0).name, server.name);
    }

    @Test
    public void testGetServer() {
        PanoptaServer result = defaultPanoptaService.getServer(vmId);
        verify(panoptaDataService).getPanoptaDetails(vmId);
        verify(panoptaApiServerService).getServer(serverId, partnerCustomerKey);
        assertEquals(serverId, result.serverId);
        assertEquals(serverKey, result.serverKey);
        Instant instant = Instant.parse("2021-01-19T17:22:20Z");
        assertEquals(instant, result.agentLastSynced);
    }

    @Test
    public void testDeleteServer() {
        defaultPanoptaService.deleteServer(vmId);
        verify(panoptaApiServerService).deleteServer(serverId, partnerCustomerKey);
    }

    @Test
    public void testGetAvailability() throws PanoptaServiceException {
        String startTime = "2007-12-03 10:15:30";
        String endTime = "2007-12-03 12:15:30";
        defaultPanoptaService.getAvailability(vmId, startTime, endTime);
        verify(panoptaApiServerService).getAvailability(serverId, partnerCustomerKey, startTime, endTime);
    }

    @Test(expected = PanoptaServiceException.class)
    public void testGetAvailabilityException() throws PanoptaServiceException {
        String startTime = "2009-04-03 10:15:30";
        String endTime = "2009-05-03 12:15:30";
        defaultPanoptaService.getAvailability(UUID.randomUUID(), startTime, endTime);
    }

    @Test
    public void testGetServers() {
        List<PanoptaServer> panoptaServersList = defaultPanoptaService.getServers(shopperId, ipAddress, orionGuid);
        verify(panoptaApiServerService).getServers(partnerCustomerKey, ipAddress, orionGuid.toString(), "active");
        assertNotNull(panoptaServersList);
        assertTrue(panoptaServersList.size() > 0);
    }

    @Test
    public void testGetServersReturnsEmptyList() {
        PanoptaServers panoptaServers = new PanoptaServers();
        panoptaServers.servers = new ArrayList<>();
        when(panoptaApiServerService.getServers(partnerCustomerKey, ipAddress, orionGuid.toString(), "active"))
                .thenReturn(panoptaServers);

        List<PanoptaServer> panoptaServersList = defaultPanoptaService.getServers(shopperId, ipAddress, orionGuid);
        verify(panoptaApiServerService).getServers(partnerCustomerKey, ipAddress, orionGuid.toString(), "active");
        assertNotNull(panoptaServersList);
        assertTrue(panoptaServersList.isEmpty());
    }

    @Test
    public void testGetOutage() throws PanoptaServiceException {
        PanoptaOutage mockOutage = new PanoptaOutage();
        mockOutage.outageId = 123;
        mockOutage.started = Instant.now().minus(5, ChronoUnit.HOURS);
        mockOutage.metricIds = Collections.singleton(456L);
        when(panoptaApiOutageService.getOutage(123, partnerCustomerKey)).thenReturn(mockOutage);

        VmOutage outage = defaultPanoptaService.getOutage(vmId, 123);

        assertEquals(mockOutage.outageId, outage.panoptaOutageId);
        assertEquals(mockOutage.started, outage.started);
        assertEquals(vmId, outage.vmId);
        verify(panoptaApiServerService, times(1)).getUsageList(serverId, partnerCustomerKey, 0);
        verify(panoptaApiServerService, times(1)).getNetworkList(serverId, partnerCustomerKey,  0);
        verify(panoptaApiOutageService, times(1)).getOutage(mockOutage.outageId, partnerCustomerKey);
    }

    @Test
    public void testGetOutageMapsTypeIdToVmOutage() throws PanoptaServiceException {
        PanoptaMetricId metricId = networkIdList.value.get(0);
        PanoptaOutage mockOutage = mock(PanoptaOutage.class);
        mockOutage.metricIds = Collections.singleton(metricId.id);
        mockOutage.networkMetricMetadata = new HashMap<>();
        when(panoptaMetricMapper.getVmMetric(metricId.typeId)).thenReturn(VmMetric.PING);
        when(panoptaApiOutageService.getOutage(123, partnerCustomerKey)).thenReturn(mockOutage);

        VmOutage outage = defaultPanoptaService.getOutage(vmId, 123);

        assertTrue(outage.metrics.contains(VmMetric.PING));
        verify(panoptaMetricMapper, times(1)).getVmMetric(metricId.typeId);
    }

    @Test
    public void testGetOutageOnlyReturnsAffectedMetrics() throws PanoptaServiceException {
        PanoptaMetricId affectedMetric = networkIdList.value.get(0);
        PanoptaMetricId unaffectedMetric = networkIdList.value.get(1);
        PanoptaOutage mockOutage = mock(PanoptaOutage.class);
        mockOutage.metricIds = Collections.singleton(affectedMetric.id);
        mockOutage.networkMetricMetadata = new HashMap<>();
        when(panoptaMetricMapper.getVmMetric(affectedMetric.typeId)).thenReturn(VmMetric.HTTP);
        when(panoptaMetricMapper.getVmMetric(unaffectedMetric.typeId)).thenReturn(VmMetric.SSH);
        when(panoptaApiOutageService.getOutage(123, partnerCustomerKey)).thenReturn(mockOutage);

        VmOutage outage = defaultPanoptaService.getOutage(vmId, 123);

        assertTrue(outage.metrics.contains(VmMetric.HTTP));
        assertFalse(outage.metrics.contains(VmMetric.SSH));
        verify(panoptaMetricMapper, times(1)).getVmMetric(affectedMetric.typeId);
        verify(panoptaMetricMapper, never()).getVmMetric(unaffectedMetric.typeId);
    }


    @Test
    public void testGetOutageReturnsNetworkMetadata() throws PanoptaServiceException {
        PanoptaMetricId affectedMetric = networkIdList.value.get(0);
        PanoptaMetricId unaffectedMetric = networkIdList.value.get(1);
        PanoptaOutage mockOutage = mock(PanoptaOutage.class);
        mockOutage.metricIds = Collections.singleton(affectedMetric.id);
        Map<Long, List<String>> networkMetricMetadata = new HashMap<>();
        networkMetricMetadata.put(affectedMetric.id, Arrays.asList("Unable to resolve host name additionalFqdn.fake"));
        mockOutage.networkMetricMetadata = networkMetricMetadata;
        when(panoptaMetricMapper.getVmMetric(affectedMetric.typeId)).thenReturn(VmMetric.HTTP);
        when(panoptaMetricMapper.getVmMetric(unaffectedMetric.typeId)).thenReturn(VmMetric.SSH);
        when(panoptaApiOutageService.getOutage(123, partnerCustomerKey)).thenReturn(mockOutage);

        VmOutage outage = defaultPanoptaService.getOutage(vmId, 123);

        assertEquals(Arrays.asList("Unable to resolve host name additionalFqdn.fake"), outage.domainMonitoringMetadata.get(0).metadata);
        assertEquals("additionalFqdn.fake", outage.domainMonitoringMetadata.get(0).additionalFqdn);
        assertEquals(VmMetric.HTTP, outage.domainMonitoringMetadata.get(0).metric);

        verify(panoptaMetricMapper, times(3)).getVmMetric(affectedMetric.typeId);
        verify(panoptaMetricMapper, never()).getVmMetric(unaffectedMetric.typeId);
    }

    @Test
    public void testGetOutageForUnknownServerId() {
        try {
            defaultPanoptaService.getOutage(UUID.randomUUID(), 123);
            fail();
        } catch (PanoptaServiceException e) {
            assertEquals("NO_SERVER_FOUND", e.getId());
        }
        verify(panoptaApiServerService, never()).getUsageList(anyLong(), anyString(), anyInt());
        verify(panoptaApiServerService, never()).getNetworkList(anyLong(), anyString(), anyInt());
        verify(panoptaApiServerService, never()).getOutages(anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    public void testGetOutageForUnknownOutageId() {
        when(panoptaApiOutageService.getOutage(123, partnerCustomerKey)).thenThrow(new NotFoundException());
        try {
            defaultPanoptaService.getOutage(vmId, 123);
            fail();
        } catch (PanoptaServiceException e) {
            assertEquals("NO_OUTAGE_FOUND", e.getId());
        }
        verify(panoptaApiServerService, never()).getUsageList(anyLong(), anyString(), anyInt());
        verify(panoptaApiServerService, never()).getNetworkList(anyLong(), anyString(), anyInt());
        verify(panoptaApiServerService, never()).getOutages(anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    public void testDeleteAdditionalFqdnCallsUpdateServer() throws PanoptaServiceException {
        defaultPanoptaService.deleteAdditionalFqdnFromServer(vmId, "definitelyFakeFQDN.here");
        verify(panoptaApiServerService).updateServer(eq(serverId), eq(partnerCustomerKey), updateServerRequest.capture());
        PanoptaApiUpdateServerRequest request = updateServerRequest.getValue();

        assertEquals(1, request.additionalFqdns.size());
        assertTrue(request.additionalFqdns.contains("definitelyFakeFQDN2.here"));
    }

    @Test
    public void testDeleteAdditionalFqdnThrowsErrorIfServerDoesNotExist() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        try {
            defaultPanoptaService.deleteAdditionalFqdnFromServer(vmId, "thisfqdn.istotallymostdefinitely.fake");
            fail();
        } catch (PanoptaServiceException e) {
            assertEquals("NO_SERVER_FOUND", e.getId());
        }
    }

    @Test
    public void testAddAdditionalFqdnCallsUpdateServer() throws PanoptaServiceException {
        defaultPanoptaService.addAdditionalFqdnToServer(vmId, "thisfqdn.istotallymostdefinitely.fake");
        verify(panoptaApiServerService).updateServer(eq(serverId), eq(partnerCustomerKey), updateServerRequest.capture());
        PanoptaApiUpdateServerRequest request = updateServerRequest.getValue();
        assertEquals(3, request.additionalFqdns.size());

        assertTrue(request.additionalFqdns.contains("thisfqdn.istotallymostdefinitely.fake"));
        assertTrue(request.additionalFqdns.contains("definitelyFakeFQDN.here"));
        assertTrue(request.additionalFqdns.contains("definitelyFakeFQDN2.here"));
    }

    @Test
    public void testAddAdditionalFqdnThrowsErrorIfServerDoesNotExist() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        try {
            defaultPanoptaService.addAdditionalFqdnToServer(vmId, "thisfqdn.istotallymostdefinitely.fake");
            fail();
        } catch (PanoptaServiceException e) {
            assertEquals("NO_SERVER_FOUND", e.getId());
        }
    }

    @Test
    public void testAddNetworkServiceServerHTTPCallsAddNetworkService() throws PanoptaServiceException {
        defaultPanoptaService.addNetworkService(vmId, VmMetric.HTTP, "thisfqdn.istotallymostdefinitely.fake", 1, true);
        verify(panoptaApiServerService).addNetworkService(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testAddNetworkServiceServerHTTPSCallsAddNetworkService() throws PanoptaServiceException {
        defaultPanoptaService.addNetworkService(vmId, VmMetric.HTTPS, "thisfqdn.istotallymostdefinitely.fake", 1, true);
        verify(panoptaApiServerService).addNetworkService(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testAddNetworkServiceServerCallsAddNetworkServiceIsManagedFalse() throws PanoptaServiceException {
        defaultPanoptaService.addNetworkService(vmId, VmMetric.HTTPS, "thisfqdn.istotallymostdefinitely.fake", 1, false);
        verify(panoptaApiServerService).addNetworkService(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testAddNetworkServiceServerCallsAddNetworkServiceIsManagedFalseMonitoringTrue() throws PanoptaServiceException {
        defaultPanoptaService.addNetworkService(vmId, VmMetric.HTTPS, "thisfqdn.istotallymostdefinitely.fake", 1, false);
        verify(panoptaApiServerService).addNetworkService(eq(serverId), eq(partnerCustomerKey), any());
    }
    
    @Test
    public void testAddNetworkServiceThrowsErrorIfMetricIsNotHTTPOrHTTPS() {
        try {
            defaultPanoptaService.addNetworkService(vmId, VmMetric.PING, "thisfqdn.istotallymostdefinitely.fake", 1, true);
            fail();
        } catch (PanoptaServiceException e) {
            assertEquals("UNKNOWN_METRIC", e.getId());
        }
    }

    @Test
    public void testDeleteNetworkServiceCallsDeleteNetworkService() throws PanoptaServiceException {
        defaultPanoptaService.deleteNetworkService(vmId, 3L);
        verify(panoptaApiServerService).deleteNetworkService(eq(serverId), eq(3L), eq(partnerCustomerKey));
    }

    @Test
    public void testDeleteNetworkServiceThrowsErrorIfServerDoesNotExist() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        try {
            defaultPanoptaService.deleteNetworkService(vmId, 3L);
            fail();
        } catch (PanoptaServiceException e) {
            assertEquals("NO_SERVER_FOUND", e.getId());
        }
    }
}
