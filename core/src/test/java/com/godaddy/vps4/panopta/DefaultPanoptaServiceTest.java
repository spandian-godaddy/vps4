package com.godaddy.vps4.panopta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
    private PanoptaDataService panoptaDataService;
    private VirtualMachineService virtualMachineService;
    private CreditService creditService;
    private VirtualMachineCredit credit;
    private VirtualMachine virtualMachine;
    private Config config;
    private Cache cache;
    private int serverId;
    private UUID vmId;
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
    @Inject
    private PanoptaCustomerRequest panoptaCustomerRequest;
    @Inject
    private PanoptaApiCustomerRequest panoptaApiCustomerRequest;

    @Before
    public void setup() {
        pool = spy(Executors.newCachedThreadPool());
        cacheManager = mock(CacheManager.class);
        panoptaApiServerService = mock(PanoptaApiServerService.class);
        panoptaApiCustomerService = mock(PanoptaApiCustomerService.class);
        panoptaDataService = mock(PanoptaDataService.class);
        panoptaApiCustomerList = mock(PanoptaApiCustomerList.class);
        panoptaCustomerDetails = mock(PanoptaCustomerDetails.class);
        virtualMachineService = mock(VirtualMachineService.class);
        creditService = mock(CreditService.class);
        config = mock(Config.class);
        cache = mock(Cache.class);
        credit = createDummyCredit();
        virtualMachine = new VirtualMachine();
        virtualMachine.orionGuid = UUID.randomUUID();
        serverId = 42;
        partnerCustomerKey = "gdtest_" + shopperId;
        customerKey = "someCustomerKey";
        serverKey = "someServerKey";
        panoptaDetail = new PanoptaDetail(vmId, partnerCustomerKey,
                                          customerKey, serverId, serverKey,
                                          Instant.now(), Instant.MAX);
        panoptaServers = new PanoptaServers();
        panoptaServers.servers = new ArrayList<>();
        server = new PanoptaServers.Server();
        server.url = "https://api2.panopta.com/v2/server/666";
        server.serverKey = "someServerKey";
        server.name = "someServerName";
        server.fqdn = "someFqdn";
        server.serverGroup = "someServerGroup";
        server.status = PanoptaServer.Status.ACTIVE.toString();
        panoptaServers.servers.add(server);
        panoptaCustomerRequest = new PanoptaCustomerRequest(virtualMachineService, creditService, config);
        panoptaApiCustomerRequest = new PanoptaApiCustomerRequest();
        setupGraphIdLists();
        setupGraphs();
        when(cacheManager.getCache(CacheName.PANOPTA_METRIC_GRAPH,
                                   String.class,
                                   DefaultPanoptaService.CachedMonitoringGraphs.class)).thenReturn(cache);
        defaultPanoptaService = new DefaultPanoptaService(pool,
                                                          cacheManager,
                                                          panoptaApiCustomerService,
                                                          panoptaApiServerService,
                                                          panoptaDataService,
                                                          panoptaCustomerRequest,
                                                          config);
    }

    @After
    public void teardown() {
        pool.shutdown();
    }

    private void setupGraphIdLists() {
        List<PanoptaGraphId> graphIdList = new ArrayList<>();
        PanoptaGraphId pgi1 = new PanoptaGraphId();
        pgi1.id = (int) (Math.random() * 9999);
        pgi1.type = VmMetric.UNKNOWN;
        graphIdList.add(pgi1);
        PanoptaGraphId pgi2 = new PanoptaGraphId();
        pgi2.id = (int) (Math.random() * 9999);
        pgi2.type = VmMetric.HTTP;
        graphIdList.add(pgi2);
        usageIdList = new PanoptaUsageIdList();
        usageIdList.setList(graphIdList);
        networkIdList = new PanoptaNetworkIdList();
        networkIdList.setList(graphIdList);
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

    private String mockedupCustomerList() {
        return "{\n" +
                "  \"customer_list\": [\n" +
                "    {\n" +
                "      \"customer_key\": \"2hum-wpmt-vswt-2g3b\",\n" +
                "      \"email_address\": \"abhoite@godaddy.com\",\n" +
                "      \"name\": \"Godaddy VPS4 POC\",\n" +
                "      \"package\": \"godaddy.fully_managed\",\n" +
                "      \"partner_customer_key\": \"gdtest_" + vmId + "\",\n" +
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
    }

    private VirtualMachineCredit createDummyCredit() {
        return new VirtualMachineCredit.Builder(mock(DataCenterService.class))
                .withAccountGuid(UUID.randomUUID().toString())
                .withAccountStatus(AccountStatus.ACTIVE)
                .withShopperID("dummy-shopper-id")
                .build();
    }

    @Test(expected = PanoptaServiceException.class)
    public void testInvokesCreatesCustomer() throws PanoptaServiceException {
        when(virtualMachineService.getVirtualMachine(any(UUID.class))).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(any(UUID.class))).thenReturn(credit);
        when(config.get("panopta.api.partner.customer.key.prefix")).thenReturn("gdtest_");
        when(panoptaApiCustomerService.getCustomer(eq(partnerCustomerKey))).thenReturn(panoptaApiCustomerList);
        doNothing().when(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));

        defaultPanoptaService.createCustomer(shopperId);

        verify(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));
    }

    @Test
    public void testInvokesCreatesCustomerWithMatchingCustomer() throws PanoptaServiceException, IOException {
        PanoptaApiCustomerList fakePanoptaCustomers =
                objectMapper.readValue(mockedupCustomerList(), PanoptaApiCustomerList.class);
        when(virtualMachineService.getVirtualMachine(any(UUID.class))).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(any(UUID.class))).thenReturn(credit);
        when(config.get("panopta.api.partner.customer.key.prefix")).thenReturn("gdtest_");
        when(panoptaApiCustomerService.getCustomer(eq("gdtest_" + vmId))).thenReturn(fakePanoptaCustomers);
        when(panoptaApiCustomerList.getCustomerList()).thenReturn(fakePanoptaCustomers.getCustomerList());
        doNothing().when(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));

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
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        when(panoptaApiServerService.getUsageList(serverId, partnerCustomerKey, 0)).thenReturn(usageIdList);

        List<PanoptaGraphId> ids = defaultPanoptaService.getUsageIds(vmId);

        verify(panoptaApiServerService).getUsageList(serverId, partnerCustomerKey, 0);
        assertEquals(ids.size(), 1);
        assertEquals(ids.get(0).id, usageIdList.getList().get(0).id);
        assertEquals(ids.get(0).type, usageIdList.getList().get(0).type);
    }

    @Test
    public void testGetNetworkIdsWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        defaultPanoptaService.getNetworkIds(vmId);
        verify(panoptaApiServerService, never()).getNetworkList(anyInt(), anyString(), anyInt());
    }

    @Test
    public void testGetNetworkIds() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        when(panoptaApiServerService.getNetworkList(serverId, partnerCustomerKey, 0)).thenReturn(networkIdList);

        List<PanoptaGraphId> ids = defaultPanoptaService.getNetworkIds(vmId);

        verify(panoptaApiServerService).getNetworkList(serverId, partnerCustomerKey, 0);
        assertEquals(ids.size(), 1);
        assertEquals(ids.get(0).id, networkIdList.getList().get(0).id);
        assertEquals(ids.get(0).type, networkIdList.getList().get(0).type);
    }

    @Test
    public void testGetUsageGraphs() throws PanoptaServiceException, InterruptedException {
        when(cache.containsKey(String.format("%s.usage.%s", vmId, "hour"))).thenReturn(false);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        when(panoptaApiServerService.getUsageList(serverId, partnerCustomerKey, 0)).thenReturn(usageIdList);
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
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        when(panoptaApiServerService.getNetworkList(serverId, partnerCustomerKey, 0)).thenReturn(networkIdList);
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
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);

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
        when(cache.containsKey(String.format("%s.network.%s", vmId, "hour"))).thenReturn(true);
        when(cache.get(String.format("%s.network.%s", vmId, "hour"))).thenReturn(cachedGraphs);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);

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
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        server.status = "suspended";
        when(panoptaApiServerService.getServer(serverId, partnerCustomerKey)).thenReturn(server);
        defaultPanoptaService.pauseServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).setServerStatus(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testPauseMonitoringSuccess() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        server.status = "active";
        when(panoptaApiServerService.getServer(serverId, partnerCustomerKey)).thenReturn(server);
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
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        server.status = "active";
        when(panoptaApiServerService.getServer(serverId, partnerCustomerKey)).thenReturn(server);
        defaultPanoptaService.resumeServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).setServerStatus(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testResumeMonitoringSuccess() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        server.status = "suspended";
        when(panoptaApiServerService.getServer(serverId, partnerCustomerKey)).thenReturn(server);
        defaultPanoptaService.resumeServerMonitoring(vmId);
        verify(panoptaApiServerService).setServerStatus(eq(serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void removeMonitoringCallsPanoptaApiDeleteServer() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        defaultPanoptaService.removeServerMonitoring(vmId);
        verify(panoptaApiServerService).deleteServer(serverId, partnerCustomerKey);
    }

    @Test
    public void deleteCustomerCallsPanoptaApiDeleteCustomer() {
        when(panoptaDataService.getPanoptaCustomerDetails(shopperId)).thenReturn(panoptaCustomerDetails);
        when(panoptaCustomerDetails.getCustomerKey()).thenReturn(customerKey);
        defaultPanoptaService.deleteCustomer(shopperId);
        verify(panoptaApiCustomerService).deleteCustomer(customerKey);
    }

    @Test
    public void testGetAvailability() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(panoptaDetail);
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
    public void testGetOutage() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(panoptaDetail);
        String startTime = "2007-08-12 07:12:20";
        String endTime = "2007-08-12 08:12:20";
        int limit = 5, offset = 15;
        defaultPanoptaService.getOutage(vmId, startTime, endTime, limit, offset);
        verify(panoptaApiServerService).getOutage(serverId, partnerCustomerKey, startTime, endTime, limit, offset);
    }

    @Test(expected = PanoptaServiceException.class)
    public void testGetOutageException() throws PanoptaServiceException {
        String startTime = "2010-11-03 14:15:50";
        String endTime = "2010-11-03 17:15:50";
        int limit = 5, offset = 15;
        defaultPanoptaService.getOutage(UUID.randomUUID(), startTime, endTime, limit, offset);
    }

    @Test
    public void testGetServer() throws PanoptaServiceException {
        when(config.get("panopta.api.partner.customer.key.prefix")).thenReturn("gdtest_");
        when(panoptaApiServerService.getPanoptaServers(eq(partnerCustomerKey), eq(serverKey))).thenReturn(panoptaServers);
        PanoptaServer server = defaultPanoptaService.getServer(shopperId, serverKey);
        verify(panoptaApiServerService).getPanoptaServers(eq(partnerCustomerKey), eq(serverKey));
        assertNotNull(server);
        assertEquals(serverKey, server.serverKey);
    }

    @Test(expected =  PanoptaServiceException.class)
    public void testGetServerThrowsException() throws PanoptaServiceException {
        PanoptaServers panoptaServers = new PanoptaServers();
        panoptaServers.servers = new ArrayList<>();
        when(panoptaApiServerService.getPanoptaServers(eq(partnerCustomerKey), eq(serverKey))).thenReturn(panoptaServers);
        PanoptaServer server = defaultPanoptaService.getServer(shopperId, serverKey);
        verify(panoptaApiServerService).getPanoptaServers(eq(partnerCustomerKey), eq(serverKey));
        assertNull(server);
    }
}
