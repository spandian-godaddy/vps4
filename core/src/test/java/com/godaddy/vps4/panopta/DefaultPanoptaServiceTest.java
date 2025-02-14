package com.godaddy.vps4.panopta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.ws.rs.NotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
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
    @Captor private ArgumentCaptor<PanoptaApiApplyTemplateRequest> applyTemplateRequest;
    @Captor private ArgumentCaptor<PanoptaApiUpdateCustomerRequest> updateCustomerRequest;

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
        customerKey = "2hum-wpmt-vswt-2g3b";
        serverKey = "someServerKey";
        panoptaDetail = new PanoptaDetail(vmId, partnerCustomerKey,
                                          customerKey, serverId, serverKey,
                                          Instant.now(), Instant.MAX, null);
        setupPanoptaServers();
        setupGraphIdLists();
        setupGraphs();
        setupCustomers();
        setupServerGroups();
        setupPanoptaMetrics();
        setupOtherMocks();
        when(cacheManager.getCache(CacheName.PANOPTA_METRIC_GRAPH,
                                   String.class,
                                   DefaultPanoptaService.CachedMonitoringGraphs.class)).thenReturn(cache);
        defaultPanoptaService = spy(new DefaultPanoptaService(pool,
                                                          cacheManager,
                                                          panoptaApiCustomerService,
                                                          panoptaApiServerService,
                                                          panoptaApiServerGroupService,
                                                          panoptaApiOutageService,
                                                          panoptaDataService,
                                                          config,
                                                          panoptaMetricMapper));
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

    private VirtualMachineCredit createDummyCredit() {
        return new VirtualMachineCredit.Builder()
                .withAccountGuid(orionGuid.toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withShopperID(shopperId)
                .build();
    }

    private PanoptaOutage createDummyOutage(long metricId, Instant started, String reason) {
        Set<Long> metricIds = new HashSet<>();
        metricIds.add(metricId);

        PanoptaOutage outage = new PanoptaOutage();
        outage.outageId = -123456789;
        outage.started = started;
        outage.ended = Instant.now();
        outage.reason = reason;
        outage.severity = "critical";
        outage.status = "resolved";
        outage.metricIds = metricIds;
        outage.networkMetricMetadata = new HashMap<>();

        return outage;
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

    private void setupCustomers() throws IOException {
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
        when(panoptaApiCustomerService.getCustomersByStatus(partnerCustomerKey, "active")).thenReturn(fakePanoptaCustomers);
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

    private void setupPanoptaMetrics() {
        PanoptaMetricId metricIdCpu = new PanoptaMetricId();
        metricIdCpu.id = 215100054;
        metricIdCpu.typeId = 1085;
        metricIdCpu.serverInterface = null;
        metricIdCpu.status = "active";
        metricIdCpu.metadata = new HashMap<>();

        PanoptaMetricId metricIdRam = new PanoptaMetricId();
        metricIdRam.id = 215100057;
        metricIdRam.typeId = 505;
        metricIdRam.serverInterface = null;
        metricIdRam.status = "active";
        metricIdRam.metadata = new HashMap<>();

        PanoptaUsageIdList usageIdList = new PanoptaUsageIdList();
        usageIdList.value = new ArrayList<>(Arrays.asList(metricIdCpu, metricIdRam));

        when(panoptaMetricMapper.getVmMetric(metricIdCpu.typeId)).thenReturn(VmMetric.CPU);
        when(panoptaMetricMapper.getVmMetric(metricIdRam.typeId)).thenReturn(VmMetric.RAM);
        when(panoptaApiServerService.getUsageList(anyLong(), anyString(), anyInt())).thenReturn(usageIdList);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
    }

    private void setupOtherMocks() {
        when(virtualMachineService.getVirtualMachine(any(UUID.class))).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(any(UUID.class))).thenReturn(credit);
        when(config.get("panopta.api.partner.customer.key.prefix")).thenReturn("gdtest_");
        when(config.get("panopta.api.customer.email")).thenReturn("dev-vps4@godaddy.com");
        when(config.get("panopta.api.name.prefix")).thenReturn("");
        when(config.get("panopta.api.package")).thenReturn("godaddy.hosting");
        doNothing().when(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));
        when(panoptaApiServerService.getNetworkList(serverId, partnerCustomerKey, 0)).thenReturn(networkIdList);
        when(panoptaApiServerService.getServer(serverId, partnerCustomerKey)).thenReturn(server);
        when(panoptaApiServerService.getServers(partnerCustomerKey, ipAddress, orionGuid.toString(), "active"))
                .thenReturn(panoptaServers);
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
        when(panoptaApiServerService.getUsageList(serverId, partnerCustomerKey, 0)).thenReturn(usageIdList);
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
        when(panoptaMetricMapper.getVmMetric(affectedMetric.typeId)).thenReturn(VmMetric.HTTPS_DOMAIN);
        PanoptaMetricId ids = defaultPanoptaService.getNetworkIdOfAdditionalFqdn(vmId, "additionalFqdn.fake");
        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey, 0);
        assertEquals(networkIdList.value.get(0).id, ids.id);
        assertEquals(networkIdList.value.get(0).typeId, ids.typeId);
    }

    @Test
    public void testGetAdditionalFqdns() {
        Map<String, Instant> fqdnValidOnMap = new HashMap<>();
        fqdnValidOnMap.put("additionalfqdn.fake", Instant.now());
        fqdnValidOnMap.put("additionalfqdn2.fake", Instant.now());
        when(panoptaMetricMapper.getVmMetric(anyLong())).thenReturn(VmMetric.HTTPS_DOMAIN);
        when(panoptaDataService.getPanoptaAdditionalFqdnWithValidOn(vmId)).thenReturn(fqdnValidOnMap);
        List<PanoptaDomain> domains = defaultPanoptaService.getAdditionalDomains(vmId);
        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey, 0);
        assertEquals(2, domains.size());
        assertEquals(networkIdList.value.get(0).id, domains.get(0).id);
        assertEquals(networkIdList.value.get(0).typeId, domains.get(0).typeId);
    }

    @Test
    public void testGetUsageGraphs() throws PanoptaServiceException, InterruptedException {
        when(panoptaApiServerService.getUsageList(serverId, partnerCustomerKey, 0)).thenReturn(usageIdList);
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
    public void testDoesNotPauseMonitoringWhenNoDbEntry() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.pauseServerMonitoring(vmId, shopperId);
        verify(panoptaApiServerService, never()).getServer(serverId, partnerCustomerKey);
    }

    @Test
    public void testDoesNotPauseMonitoringWhenPanoptaAlreadySuspended() throws PanoptaServiceException {
        server.status = "suspended";
        defaultPanoptaService.pauseServerMonitoring(vmId, shopperId);
        verify(panoptaApiServerService, never()).updateServer(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testPauseMonitoringSuccess() throws PanoptaServiceException {
        server.status = "active";
        defaultPanoptaService.pauseServerMonitoring(vmId, shopperId);
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
    public void testGetStatus() {
        String status = defaultPanoptaService.getStatus(shopperId);
        verify(panoptaApiCustomerService).getCustomer(partnerCustomerKey);
        assertEquals(status, "active");
    }

    @Test
    public void testSetStatus() {
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(panoptaCustomerDetails);
        when(panoptaCustomerDetails.getCustomerKey()).thenReturn(customerKey);
        defaultPanoptaService.setStatus(shopperId, "active");
        verify(panoptaApiCustomerService).updateCustomer(eq(customerKey), updateCustomerRequest.capture());
        PanoptaApiUpdateCustomerRequest request = updateCustomerRequest.getValue();

        assertEquals(request.getEmailAddress(), "dev-vps4@godaddy.com");
        assertEquals(request.getName(), "dummy-shopper-id");
        assertEquals(request.getPanoptaPackage(), "godaddy.hosting");
        assertEquals(request.getStatus(), "active");
    }

    @Test
    public void testCreateServer() throws PanoptaServiceException {
        String ipAddress = virtualMachine.primaryIpAddress.ipAddress;
        PanoptaServer server = defaultPanoptaService.createServer(shopperId, orionGuid, ipAddress, null);
        verify(panoptaApiServerService).createServer(eq(partnerCustomerKey), any(PanoptaApiServerRequest.class));
        verify(panoptaApiServerService).getServers(partnerCustomerKey, ipAddress, orionGuid.toString(), "active");
        assertEquals(panoptaServers.servers.get(0).fqdn, server.fqdn);
        assertEquals(panoptaServers.servers.get(0).name, server.name);
    }

    @Test
    public void testApplyTemplates() {
        String[] templates = { "https://api2.panopta.com/v2/server_template/fake_template_base",
                "https://api2.panopta.com/v2/server_template/fake_template_dc" };

        defaultPanoptaService.applyTemplates(serverId, partnerCustomerKey, templates);
        verify(panoptaApiServerService, times(2)).applyTemplate(eq(serverId), eq(partnerCustomerKey), applyTemplateRequest.capture());
        List<PanoptaApiApplyTemplateRequest> req = applyTemplateRequest.getAllValues();

        assertEquals(templates[0], req.get(0).getServerTemplate());
        assertEquals(templates[1], req.get(1).getServerTemplate());
        assertTrue(req.get(0).isContinuous());
        assertTrue(req.get(1).isContinuous());
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
    public void testGetServerThrowsNotFoundException() throws NotFoundException {
        when(panoptaApiServerService.getServer(serverId, partnerCustomerKey)).thenThrow(new NotFoundException());

        PanoptaServer result = defaultPanoptaService.getServer(vmId);
        verify(panoptaDataService).getPanoptaDetails(vmId);
        verify(panoptaApiServerService).getServer(serverId, partnerCustomerKey);
        assertNull(result);
    }
    @Test
    public void testGetServerNotFoundInDb() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);

        PanoptaServer result = defaultPanoptaService.getServer(vmId);
        verify(panoptaDataService).getPanoptaDetails(vmId);
        verify(panoptaApiServerService, never()).getServer(serverId, partnerCustomerKey);
        assertNull(result);
    }
    @Test
    public void testDeleteServer() throws PanoptaServiceException {
        defaultPanoptaService.deleteServer(vmId, shopperId);
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
        when(panoptaMetricMapper.getVmMetric(affectedMetric.typeId)).thenReturn(VmMetric.HTTP_DOMAIN);
        when(panoptaMetricMapper.getVmMetric(unaffectedMetric.typeId)).thenReturn(VmMetric.SSH);
        when(panoptaApiOutageService.getOutage(123, partnerCustomerKey)).thenReturn(mockOutage);

        VmOutage outage = defaultPanoptaService.getOutage(vmId, 123);

        assertTrue(outage.metrics.contains(VmMetric.HTTP_DOMAIN));
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
        networkMetricMetadata.put(affectedMetric.id, Collections.singletonList("Unable to resolve host name additionalFqdn.fake"));
        mockOutage.networkMetricMetadata = networkMetricMetadata;
        when(panoptaMetricMapper.getVmMetric(affectedMetric.typeId)).thenReturn(VmMetric.HTTP_DOMAIN);
        when(panoptaMetricMapper.getVmMetric(unaffectedMetric.typeId)).thenReturn(VmMetric.SSH);
        when(panoptaApiOutageService.getOutage(123, partnerCustomerKey)).thenReturn(mockOutage);

        VmOutage outage = defaultPanoptaService.getOutage(vmId, 123);

        assertEquals(Collections.singletonList("Unable to resolve host name additionalFqdn.fake"), outage.domainMonitoringMetadata.get(0).metadata);
        assertEquals("additionalFqdn.fake", outage.domainMonitoringMetadata.get(0).additionalFqdn);
        assertEquals(VmMetric.HTTP_DOMAIN, outage.domainMonitoringMetadata.get(0).metric);

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
        verify(panoptaApiServerService, never()).getOutages(anyLong(), anyString(), anyString(), anyInt(), anyString());
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
        verify(panoptaApiServerService, never()).getOutages(anyLong(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    public void testGetOutages() throws PanoptaServiceException {
        long ramMetricId = 215100057L, cpuMetricId = 215100054L;
        PanoptaOutageList panoptaOutageList = new PanoptaOutageList();
        PanoptaOutage newRamOutage = createDummyOutage(ramMetricId, Instant.now(), "vps4_ram_outage");
        PanoptaOutage oldCpuOutage = createDummyOutage(cpuMetricId, Instant.now().minus(7, ChronoUnit.DAYS), "vps4_cpu_outage");
        panoptaOutageList.value = new ArrayList<>(Arrays.asList(newRamOutage, oldCpuOutage));

        when(panoptaApiServerService.getOutages(serverId, partnerCustomerKey, null, 0, null)).thenReturn(panoptaOutageList);

        List<VmOutage> outages = defaultPanoptaService.getOutages(vmId, null, null, null);

        assertEquals(2, outages.size());
    }

    @Test
    public void testGetMetricFilteredOutages() throws PanoptaServiceException {
        long ramMetricId = 215100057L, cpuMetricId = 215100054L;
        PanoptaOutageList panoptaOutageList = new PanoptaOutageList();
        PanoptaOutage ramOutage = createDummyOutage(ramMetricId, Instant.now(), "vps4_ram_outage");
        PanoptaOutage cpuOutage = createDummyOutage(cpuMetricId, Instant.now(), "vps4_cpu_outage");
        panoptaOutageList.value = new ArrayList<>(Arrays.asList(ramOutage, cpuOutage));

        when(panoptaApiServerService.getOutages(serverId, partnerCustomerKey, null, 0, null)).thenReturn(panoptaOutageList);

        List<VmOutage> outages = defaultPanoptaService.getOutages(vmId, null, VmMetric.RAM, null);

        assertEquals(1, outages.size());
        assertEquals(ramOutage.reason, outages.get(0).reason);
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
        defaultPanoptaService.addNetworkService(vmId, VmMetric.HTTP_DOMAIN, "thisfqdn.istotallymostdefinitely.fake", 1, true);
        verify(panoptaApiServerService).addNetworkService(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testAddNetworkServiceServerHTTPSCallsAddNetworkService() throws PanoptaServiceException {
        defaultPanoptaService.addNetworkService(vmId, VmMetric.HTTPS_DOMAIN, "thisfqdn.istotallymostdefinitely.fake", 1, true);
        verify(panoptaApiServerService).addNetworkService(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testAddNetworkServiceServerCallsAddNetworkServiceIsManagedFalse() throws PanoptaServiceException {
        defaultPanoptaService.addNetworkService(vmId, VmMetric.HTTPS_DOMAIN, "thisfqdn.istotallymostdefinitely.fake", 1, false);
        verify(panoptaApiServerService).addNetworkService(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testAddNetworkServiceServerCallsAddNetworkServiceIsManagedFalseMonitoringTrue() throws PanoptaServiceException {
        defaultPanoptaService.addNetworkService(vmId, VmMetric.HTTPS_DOMAIN, "thisfqdn.istotallymostdefinitely.fake", 1, false);
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

    @Test
    public void getOutageMetrics() throws PanoptaServiceException {
        Set<String> expected = new HashSet<>(Arrays.asList("CPU", "RAM"));
        long ramMetricId = 215100057L, cpuMetricId = 215100054L;
        PanoptaOutageList panoptaOutageList = new PanoptaOutageList();
        PanoptaOutage newRamOutage = createDummyOutage(ramMetricId, Instant.now(), "vps4_ram_outage");
        PanoptaOutage oldRamOutage = createDummyOutage(ramMetricId, Instant.now().minus(7, ChronoUnit.DAYS), "vps4_ram_outage");
        PanoptaOutage oldCpuOutage = createDummyOutage(cpuMetricId, Instant.now().minus(7, ChronoUnit.DAYS), "vps4_cpu_outage");
        panoptaOutageList.value = new ArrayList<>(Arrays.asList(newRamOutage, oldRamOutage, oldCpuOutage));

        when(panoptaApiServerService.getOutages(serverId, partnerCustomerKey, null, 0, null)).thenReturn(panoptaOutageList);

        Set<String> actual = defaultPanoptaService.getOutageMetrics(vmId);

        assertEquals(expected, actual);
    }

    @Test
    public void getOutageMetricsWithConsolidatedOutage() throws PanoptaServiceException {
        Set<String> expected = new HashSet<>(Arrays.asList("CPU", "RAM"));
        long ramMetricId = 215100057L, cpuMetricId = 215100054L;
        PanoptaOutageList panoptaOutageList = new PanoptaOutageList();
        PanoptaOutage newRamOutage = createDummyOutage(ramMetricId, Instant.now(), "vps4_ram_outage");
        PanoptaOutage oldRamAndCpuOutage = createDummyOutage(ramMetricId, Instant.now().minus(7, ChronoUnit.DAYS), "vps4_ram_outage");
        oldRamAndCpuOutage.metricIds.add(cpuMetricId);
        panoptaOutageList.value = new ArrayList<>(Arrays.asList(newRamOutage, oldRamAndCpuOutage));

        when(panoptaApiServerService.getOutages(serverId, partnerCustomerKey, null, 0, null)).thenReturn(panoptaOutageList);

        assertEquals(expected, defaultPanoptaService.getOutageMetrics(vmId));
    }

    @Test
    public void testGetPartnerCustomerKey() {
        assertEquals(partnerCustomerKey, defaultPanoptaService.getPartnerCustomerKey(shopperId));
    }

    @Test
    public void testValidateAndGetOrCreatePanoptaCustomerGetsCustomerFromDb() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(panoptaCustomerDetails);
        when(panoptaCustomerDetails.getCustomerKey()).thenReturn(customerKey);

        defaultPanoptaService.validateAndGetOrCreatePanoptaCustomer(shopperId);

        verify(panoptaDataService, times(1)).getPanoptaCustomerDetails(shopperId);
        verify(defaultPanoptaService, times(1)).getCustomer(shopperId);
        verify(defaultPanoptaService, never()).createCustomer(shopperId);
        verify(panoptaDataService, never()).createOrUpdatePanoptaCustomer(anyString(), anyString());
    }

    @Test
    public void testValidateAndGetOrCreatePanoptaCustomerCreatesPanoptaCustomer() throws PanoptaServiceException, JsonProcessingException {
        String mock = "{\n" +
                "  \"customer_list\": [],\n" +
                "  \"meta\": {\n" +
                "    \"limit\": 50,\n" +
                "    \"next\": null,\n" +
                "    \"offset\": 0,\n" +
                "    \"previous\": null,\n" +
                "    \"total_count\": 0\n" +
                "  }\n" +
                "}\n";
        PanoptaApiCustomerList fakePanoptaCustomers = objectMapper.readValue(mock, PanoptaApiCustomerList.class);
        when(panoptaApiCustomerService.getCustomersByStatus(partnerCustomerKey, "active")).thenReturn(fakePanoptaCustomers);
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(null);

        defaultPanoptaService.validateAndGetOrCreatePanoptaCustomer(shopperId);

        verify(panoptaDataService, times(2)).getPanoptaCustomerDetails(shopperId);
        verify(defaultPanoptaService, times(1)).getCustomer(shopperId);
        verify(defaultPanoptaService, times(1)).createCustomer(shopperId);
        verify(panoptaDataService, times(1)).createOrUpdatePanoptaCustomer(anyString(), anyString());
    }

    @Test
    public void testValidateAndGetOrCreatePanoptaCustomerGetsCustomerFromPanoptaAndStoresInDb() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(panoptaCustomerDetails);

        defaultPanoptaService.validateAndGetOrCreatePanoptaCustomer(shopperId);

        verify(panoptaDataService, times(2)).getPanoptaCustomerDetails(shopperId);
        verify(defaultPanoptaService, times(1)).getCustomer(shopperId);
        verify(defaultPanoptaService, never()).createCustomer(shopperId);
        verify(panoptaDataService, times(1)).createOrUpdatePanoptaCustomer(anyString(), anyString());
    }

    @Test
    public void testValidateAndGetOrCreatePanoptaCustomerCreatesCustomerAndUpdatesDbIfDestroyedInPanopta() throws PanoptaServiceException, JsonProcessingException {
        String mock = "{\n" +
                "  \"customer_list\": [],\n" +
                "  \"meta\": {\n" +
                "    \"limit\": 50,\n" +
                "    \"next\": null,\n" +
                "    \"offset\": 0,\n" +
                "    \"previous\": null,\n" +
                "    \"total_count\": 0\n" +
                "  }\n" +
                "}\n";
        PanoptaApiCustomerList fakePanoptaCustomers = objectMapper.readValue(mock, PanoptaApiCustomerList.class);
        when(panoptaApiCustomerService.getCustomersByStatus(partnerCustomerKey, "active")).thenReturn(fakePanoptaCustomers);
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(panoptaCustomerDetails);
        when(panoptaCustomerDetails.getCustomerKey()).thenReturn(null);

        defaultPanoptaService.validateAndGetOrCreatePanoptaCustomer(shopperId);

        verify(panoptaDataService, times(2)).getPanoptaCustomerDetails(shopperId);
        verify(panoptaDataService, times(1)).setAllPanoptaServersOfCustomerDestroyed(shopperId);
        verify(panoptaDataService, times(1)).checkAndSetPanoptaCustomerDestroyed(shopperId);
        verify(defaultPanoptaService, times(1)).getCustomer(shopperId);
        verify(defaultPanoptaService, times(1)).createCustomer(shopperId);
        verify(panoptaDataService, times(1)).createOrUpdatePanoptaCustomer(anyString(), anyString());
    }

    @Test
    public void testValidateAndGetOrCreatePanoptaCustomerUpdatesDbIfCustomerKeyIsOutOfSync() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(panoptaCustomerDetails);
        when(panoptaCustomerDetails.getCustomerKey()).thenReturn(customerKey+"-2");

        defaultPanoptaService.validateAndGetOrCreatePanoptaCustomer(shopperId);

        verify(panoptaDataService, times(2)).getPanoptaCustomerDetails(shopperId);
        verify(panoptaDataService, times(1)).setAllPanoptaServersOfCustomerDestroyed(shopperId);
        verify(panoptaDataService, times(1)).checkAndSetPanoptaCustomerDestroyed(shopperId);
        verify(defaultPanoptaService, times(1)).getCustomer(shopperId);
        verify(panoptaDataService, times(1)).createOrUpdatePanoptaCustomer(anyString(), anyString());
    }
}
