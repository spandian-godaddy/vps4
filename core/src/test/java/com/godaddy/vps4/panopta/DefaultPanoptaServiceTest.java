package com.godaddy.vps4.panopta;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.vm.VirtualMachineService;

public class DefaultPanoptaServiceTest {

    private PanoptaApiServerService panoptaApiServerService;
    private PanoptaApiCustomerService panoptaApiCustomerService;
    private PanoptaDataService panoptaDataService;
    private VirtualMachineService virtualMachineService;
    private CreditService creditService;
    private Config config;
    private long serverId;
    private UUID vmId;
    private String customerKey;
    private String partnerCustomerKey;
    private DefaultPanoptaService defaultPanoptaService;
    private PanoptaDetail panoptaDetail;
    private PanoptaServers.Server server;

    @Before
    public void setup() {
        panoptaApiServerService = mock(PanoptaApiServerService.class);
        panoptaApiCustomerService = mock(PanoptaApiCustomerService.class);
        panoptaDataService = mock(PanoptaDataService.class);
        virtualMachineService = mock(VirtualMachineService.class);
        creditService = mock(CreditService.class);
        config = mock(Config.class);
        serverId = 666L;
        partnerCustomerKey = "someRandomPartnerCustomerKey";
        customerKey = "someCustomerKey";
        vmId = UUID.randomUUID();
        defaultPanoptaService = new DefaultPanoptaService(panoptaApiCustomerService,
                                                          panoptaApiServerService,
                                                          panoptaDataService,
                                                          virtualMachineService,
                                                          creditService,
                                                          config);
        panoptaDetail = new PanoptaDetail(1, vmId, partnerCustomerKey,
                                          customerKey, serverId, "someServerKey",
                                          Instant.now(), Instant.MAX);
        server = new PanoptaServers.Server();
        server.url = "https://api2.panopta.com/v2/server/666";
        server.serverKey = "someServerKey";
        server.name = "someServerName";
        server.fqdn = "someFqdn";
        server.serverGroup = "someServerGroup";
    }

    @Test
    public void testDoesNotPauseMonitoringWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(null);
        defaultPanoptaService.pauseServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).getServer(eq((int)serverId), eq(partnerCustomerKey));
    }

    @Test
    public void testDoesNotPauseMonitoringWhenPanoptaAlreadySuspended() {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(panoptaDetail);
        server.status = "suspended";
        when(panoptaApiServerService.getServer(eq(666), eq(partnerCustomerKey))).thenReturn(server);
        defaultPanoptaService.pauseServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).setServerStatus(eq((int)serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testPauseMonitoringSuccess() {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(panoptaDetail);
        server.status = "active";
        when(panoptaApiServerService.getServer(eq((int)serverId), eq(partnerCustomerKey))).thenReturn(server);
        defaultPanoptaService.pauseServerMonitoring(vmId);
        verify(panoptaApiServerService, times(1)).setServerStatus(eq((int)serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testDoesNotResumeMonitoringWhenNoDbEntry() {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(null);
        defaultPanoptaService.resumeServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).getServer(eq((int)serverId), eq(partnerCustomerKey));
    }

    @Test
    public void testDoesNotResumeMonitoringWhenPanoptaAlreadyActive() {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(panoptaDetail);
        server.status = "active";
        when(panoptaApiServerService.getServer(eq((int)serverId), eq(partnerCustomerKey))).thenReturn(server);
        defaultPanoptaService.resumeServerMonitoring(vmId);
        verify(panoptaApiServerService, never()).setServerStatus(eq((int)serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testResumeMonitoringSuccess() {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(panoptaDetail);
        server.status = "suspended";
        when(panoptaApiServerService.getServer(eq((int)serverId), eq(partnerCustomerKey))).thenReturn(server);
        defaultPanoptaService.resumeServerMonitoring(vmId);
        verify(panoptaApiServerService, times(1)).setServerStatus(eq((int)serverId), eq(partnerCustomerKey), any());
    }

    @Test
    public void testGetAvailabilitySuccess() throws PanoptaServiceException {
        when(panoptaDataService.getPanoptaDetails(eq(vmId))).thenReturn(panoptaDetail);
        String startTime = "2007-12-03 10:15:30";
        String endTime = "2007-12-03 12:15:30";
        defaultPanoptaService.getAvailability(vmId, startTime, endTime);
        verify(panoptaApiServerService, times(1)).getAvailability(eq((int)serverId), eq(partnerCustomerKey), eq(startTime), eq(endTime));
    }

    @Test
    public void removeMonitoringCallsPanoptaApiDeleteServer() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        defaultPanoptaService.removeServerMonitoring(vmId);
        verify(panoptaApiServerService).deleteServer((int)serverId, partnerCustomerKey);
    }

    @Test
    public void deleteCustomerCallsPanoptaApiDeleteCustomer() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetail);
        defaultPanoptaService.deleteCustomer(vmId);
        verify(panoptaApiCustomerService).deleteCustomer(customerKey);
    }
}
