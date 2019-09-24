package com.godaddy.vps4.panopta;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cache.CacheName;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.util.ObjectMapperProvider;
import com.godaddy.vps4.vm.AccountStatus;
import com.godaddy.vps4.vm.DataCenterService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;

public class DefaultPanoptaServiceTest {

    private PanoptaApiServerService panoptaApiServerService;
    private PanoptaApiCustomerService panoptaApiCustomerService;
    private PanoptaDataService panoptaDataService;
    private VirtualMachineService virtualMachineService;
    private CreditService creditService;
    private VirtualMachineCredit credit;
    private VirtualMachine virtualMachine;
    private Config config;
    private int serverId;
    private UUID vmId;
    private String customerKey;
    private String partnerCustomerKey;
    private DefaultPanoptaService defaultPanoptaService;
    private PanoptaDetail panoptaDetail;
    private PanoptaServers panoptaServers;
    private PanoptaServers.Server server;
    private PanoptaApiCustomerList panoptaApiCustomerList;
    private PanoptaUsageIdList usageIdList;
    private PanoptaNetworkIdList networkIdList;
    private PanoptaServerMetric panoptaServerMetric;

    @Inject
    private ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Before
    public void setup() {
        panoptaApiServerService = mock(PanoptaApiServerService.class);
        panoptaApiCustomerService = mock(PanoptaApiCustomerService.class);
        panoptaDataService = mock(PanoptaDataService.class);
        panoptaApiCustomerList = mock(PanoptaApiCustomerList.class);
        panoptaServerMetric = mock(PanoptaServerMetric.class);
        virtualMachineService = mock(VirtualMachineService.class);
        creditService = mock(CreditService.class);
        credit = createDummyCredit();
        virtualMachine = new VirtualMachine();
        virtualMachine.orionGuid = UUID.randomUUID();
        config = mock(Config.class);
        serverId = 42;
        partnerCustomerKey = "someRandomPartnerCustomerKey";
        customerKey = "someCustomerKey";
        vmId = UUID.randomUUID();
        panoptaDetail = new PanoptaDetail(1, vmId, partnerCustomerKey,
                                          customerKey, serverId, "someServerKey",
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
        setupGraphIdLists();
        defaultPanoptaService = new DefaultPanoptaService(panoptaApiCustomerService,
                                                          panoptaApiServerService,
                                                          panoptaDataService,
                                                          virtualMachineService,
                                                          creditService,
                                                          config);
    }

    private void setupGraphIdLists() {
        List<PanoptaGraphId> graphIdList = new ArrayList<>();
        PanoptaGraphId pgi1 = new PanoptaGraphId();
        pgi1.id = (int) (Math.random() * 9999);
        pgi1.type = PanoptaGraph.Type.UNKNOWN;
        graphIdList.add(pgi1);
        PanoptaGraphId pgi2 = new PanoptaGraphId();
        pgi2.id = (int) (Math.random() * 9999);
        pgi2.type = PanoptaGraph.Type.HTTP;
        graphIdList.add(pgi2);
        usageIdList = new PanoptaUsageIdList();
        usageIdList.setList(graphIdList);
        networkIdList = new PanoptaNetworkIdList();
        networkIdList.setList(graphIdList);
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
        when(panoptaApiCustomerService.getCustomer(eq(partnerCustomerKey))).thenReturn(panoptaApiCustomerList);
        doNothing().when(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));

        defaultPanoptaService.createCustomer(vmId);

        verify(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));
    }


    @Test
    public void testInvokesCreatesCustomerWithMatchingCustomer() throws PanoptaServiceException, IOException {
        when(config.get("panopta.api.partner.customer.key.prefix")).thenReturn("gdtest_");
        PanoptaApiCustomerList fakePanoptaCustomers =
                objectMapper.readValue(mockedupCustomerList(), PanoptaApiCustomerList.class);
        when(virtualMachineService.getVirtualMachine(any(UUID.class))).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(any(UUID.class))).thenReturn(credit);
        when(panoptaApiCustomerService.getCustomer(eq("gdtest_" + vmId))).thenReturn(fakePanoptaCustomers);
        when(panoptaApiCustomerList.getCustomerList()).thenReturn(fakePanoptaCustomers.getCustomerList());
        doNothing().when(panoptaApiCustomerService).createCustomer(any(PanoptaApiCustomerRequest.class));

        defaultPanoptaService.createCustomer(vmId);

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
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        defaultPanoptaService.deleteCustomer(vmId);
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
    public void testGetsServerMetrics() throws PanoptaServiceException {
        when(panoptaApiServerService.getMetricData(eq(serverId), anyInt(), anyString(), eq(partnerCustomerKey))).thenReturn(panoptaServerMetric);

        defaultPanoptaService.getServerMetricsFromPanopta(123, serverId, "fake-timescale", partnerCustomerKey);

        verify(panoptaApiServerService).getMetricData(eq(serverId), eq(123), eq("fake-timescale"), eq(partnerCustomerKey));

    }

    @Test(expected = PanoptaServiceException.class)
    public void testGetsServerMetricsThrowsException() throws  PanoptaServiceException {
        when(panoptaApiServerService.getMetricData(eq(serverId), anyInt(), anyString(), eq(partnerCustomerKey))).thenThrow(new RuntimeException());
        defaultPanoptaService.getServerMetricsFromPanopta(123, serverId, "fake-timescale", partnerCustomerKey);
    }

    @Test
    public void testGetServer() throws PanoptaServiceException {
        when(panoptaApiServerService.getPanoptaServers(eq(partnerCustomerKey))).thenReturn(panoptaServers);
        defaultPanoptaService.getServer(partnerCustomerKey);
        verify(panoptaApiServerService).getPanoptaServers(eq(partnerCustomerKey));
    }

    @Test(expected =  PanoptaServiceException.class)
    public void testGetServerThrowsException() throws PanoptaServiceException {
        when(panoptaApiServerService.getPanoptaServers(eq(partnerCustomerKey))).thenReturn(null);
        defaultPanoptaService.getServer(partnerCustomerKey);
        verify(panoptaApiServerService).getPanoptaServers(eq(partnerCustomerKey));
    }

    @Test(expected =  PanoptaServiceException.class)
    public void testGetServerThrowsExceptionWhenMultipleServersFound() throws PanoptaServiceException {
        panoptaServers.servers.add(server); // force add another entry to the list
        when(panoptaApiServerService.getPanoptaServers(eq(partnerCustomerKey))).thenReturn(panoptaServers);
        defaultPanoptaService.getServer(partnerCustomerKey);
        verify(panoptaApiServerService).getPanoptaServers(eq(partnerCustomerKey));
    }
}
