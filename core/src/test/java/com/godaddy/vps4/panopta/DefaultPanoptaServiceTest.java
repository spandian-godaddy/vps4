package com.godaddy.vps4.panopta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
import com.google.inject.Inject;

public class DefaultPanoptaServiceTest {
    private @PanoptaExecutorService ExecutorService pool;
    private CacheManager cacheManager;
    private PanoptaApiServerService panoptaApiServerService;
    private PanoptaApiCustomerService panoptaApiCustomerService;
    private PanoptaApiServerGroupService panoptaApiServerGroupService;
    private PanoptaDataService panoptaDataService;
    private VirtualMachineService virtualMachineService;
    private CreditService creditService;
    private Config config;
    private Cache cache;
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
    private PanoptaCustomerDetails panoptaCustomerDetails;
    private PanoptaServers panoptaServers;
    private PanoptaServers.Server server;
    private PanoptaApiCustomerList panoptaApiCustomerList;
    private PanoptaUsageIdList usageIdList;
    private PanoptaNetworkIdList networkIdList;
    private PanoptaUsageGraph usageGraph;
    private PanoptaNetworkGraph networkGraph;
    @Inject
    private ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Before
    public void setup() throws IOException {
        pool = spy(Executors.newCachedThreadPool());
        cacheManager = mock(CacheManager.class);
        panoptaApiServerService = mock(PanoptaApiServerService.class);
        panoptaApiCustomerService = mock(PanoptaApiCustomerService.class);
        panoptaApiServerGroupService = mock(PanoptaApiServerGroupService.class);
        panoptaDataService = mock(PanoptaDataService.class);
        panoptaApiCustomerList = mock(PanoptaApiCustomerList.class);
        panoptaCustomerDetails = mock(PanoptaCustomerDetails.class);
        virtualMachineService = mock(VirtualMachineService.class);
        creditService = mock(CreditService.class);
        config = mock(Config.class);
        cache = mock(Cache.class);
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
                                                          panoptaDataService,
                                                          config);
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
        panoptaServers.servers.add(server);
    }

    private void setupGraphIdLists() {
        List<PanoptaGraphId> graphIdList = new ArrayList<>();
        PanoptaGraphId pgi = new PanoptaGraphId();
        pgi.id = (int) (Math.random() * 9999);
        pgi.type = VmMetric.HTTP;
        graphIdList.add(pgi);
        usageIdList = new PanoptaUsageIdList();
        usageIdList.value = graphIdList;
        networkIdList = new PanoptaNetworkIdList();
        networkIdList.value = graphIdList;
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
        when(panoptaApiServerService.getServers(partnerCustomerKey, ipAddress, orionGuid.toString()))
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
    public void testInvokesCreatesCustomerWithMatchingCustomer() throws PanoptaServiceException, IOException {
        defaultPanoptaService.createCustomer(shopperId);
        verify(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));
    }

    @Test
    public void testGetUsageIdsWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.getUsageIds(vmId);
        verify(panoptaApiServerService, never()).getUsageList(anyInt(), anyString(), anyInt());
    }

    @Test
    public void testGetUsageIds() {
        List<PanoptaGraphId> ids = defaultPanoptaService.getUsageIds(vmId);
        verify(panoptaApiServerService).getUsageList(serverId, partnerCustomerKey, 0);
        assertEquals(ids.size(), 1);
        assertEquals(ids.get(0).id, usageIdList.value.get(0).id);
        assertEquals(ids.get(0).type, usageIdList.value.get(0).type);
    }

    @Test
    public void testGetNetworkIdsWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.getNetworkIds(vmId);
        verify(panoptaApiServerService, never()).getNetworkList(anyInt(), anyString(), anyInt());
    }

    @Test
    public void testGetNetworkIds() {
        List<PanoptaGraphId> ids = defaultPanoptaService.getNetworkIds(vmId);
        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey, 0);
        assertEquals(ids.size(), 1);
        assertEquals(ids.get(0).id, networkIdList.value.get(0).id);
        assertEquals(ids.get(0).type, networkIdList.value.get(0).type);
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

        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey, 0);
        verify(panoptaApiServerService)
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
        verify(panoptaApiServerService, never()).setServerStatus(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testPauseMonitoringSuccess() {
        server.status = "active";
        defaultPanoptaService.pauseServerMonitoring(vmId);
        verify(panoptaApiServerService).setServerStatus(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testDoesNotResumeMonitoringWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.resumeServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).getServer(serverId, partnerCustomerKey);
    }

    @Test
    public void testDoesNotResumeMonitoringWhenPanoptaAlreadyActive() {
        server.status = "active";
        defaultPanoptaService.resumeServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).setServerStatus(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testResumeMonitoringSuccess() {
        server.status = "suspended";
        defaultPanoptaService.resumeServerMonitoring(vmId);
        verify(panoptaApiServerService).setServerStatus(eq(serverId), eq(partnerCustomerKey), any());
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
        String[] templates = new String[] {
                "https://api2.panopta.com/v2/server_template/1",
                "https://api2.panopta.com/v2/server_template/2"
        };
        PanoptaServer server = defaultPanoptaService.createServer(shopperId, orionGuid, ipAddress, templates);
        verify(panoptaApiServerService).createServer(eq(partnerCustomerKey), any(PanoptaApiServerRequest.class));
        verify(panoptaApiServerService).getServers(partnerCustomerKey, ipAddress, orionGuid.toString());
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
        verify(panoptaApiServerService).getServers(partnerCustomerKey, ipAddress, orionGuid.toString());
        assertNotNull(panoptaServersList);
        assertTrue(panoptaServersList.size() > 0);
    }

    @Test
    public void testGetServersReturnsEmptyList() {
        PanoptaServers panoptaServers = new PanoptaServers();
        panoptaServers.servers = new ArrayList<>();
        when(panoptaApiServerService.getServers(partnerCustomerKey, ipAddress, orionGuid.toString()))
                .thenReturn(panoptaServers);

        List<PanoptaServer> panoptaServersList = defaultPanoptaService.getServers(shopperId, ipAddress, orionGuid);
        verify(panoptaApiServerService).getServers(partnerCustomerKey, ipAddress, orionGuid.toString());
        assertNotNull(panoptaServersList);
        assertTrue(panoptaServersList.isEmpty());
    }
}
