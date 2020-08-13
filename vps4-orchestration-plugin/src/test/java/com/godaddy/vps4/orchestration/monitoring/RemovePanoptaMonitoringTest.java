package com.godaddy.vps4.orchestration.monitoring;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;

public class RemovePanoptaMonitoringTest {

    CommandContext context = mock(CommandContext.class);
    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    PanoptaService panoptaService = mock(PanoptaService.class);
    CreditService creditService = mock(CreditService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    PanoptaDetail panoptaDetails = mock(PanoptaDetail.class);
    VirtualMachine virtualMachine = mock(VirtualMachine.class);
    String shopperId = "fake-shopper-id";
    UUID vmId = UUID.randomUUID();
    List<PanoptaServer> panoptaServerList;

    RemovePanoptaMonitoring command = new RemovePanoptaMonitoring(creditService,
                                                                  panoptaDataService,
                                                                  panoptaService,
                                                                  virtualMachineService);

    @Before
    public void setUp() {
        virtualMachine.orionGuid = UUID.randomUUID();
        panoptaServerList = new ArrayList<>();
        panoptaServerList.add(getTestPanoptaServer(virtualMachine.orionGuid.toString()));
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetails);
        when(panoptaService.getActiveServers(shopperId)).thenReturn(panoptaServerList);
        when(panoptaDataService.checkAndSetPanoptaCustomerDestroyed(shopperId)).thenReturn(true);
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);
        when(credit.getShopperId()).thenReturn(shopperId);
    }

    private PanoptaServer getTestPanoptaServer() {
        return getTestPanoptaServer(UUID.randomUUID().toString());
    }

    private PanoptaServer getTestPanoptaServer(String name) {
        PanoptaServer server = mock(PanoptaServer.class);
        server.serverId = (long) (Math.random() * 10000);
        server.name = name;
        return server;
    }

    @Test
    public void removesPanoptaWithDb() {
        when(panoptaService.getActiveServers(shopperId)).thenReturn(Collections.emptyList());
        command.execute(context, vmId);
        verify(panoptaService, times(1)).removeServerMonitoring(vmId);
        verify(panoptaDataService, times(1)).setPanoptaServerDestroyed(vmId);
    }

    @Test
    public void skipsRemovingPanoptaWithDbIfNoRecordIsFound() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        command.execute(context, vmId);
        verify(panoptaService, never()).removeServerMonitoring(vmId);
        verify(panoptaDataService, never()).setPanoptaServerDestroyed(vmId);
    }

    @Test
    public void removesPanoptaWithApi() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        long serverId = panoptaServerList.get(0).serverId;
        command.execute(context, vmId);
        verify(panoptaService, times(1)).removeServerMonitoring(serverId, shopperId);
        verify(context, times(1)).execute(DeletePanoptaCustomer.class, shopperId);
    }

    @Test
    public void onlyRemovesMatchingServers() {
        panoptaServerList.add(getTestPanoptaServer());
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        command.execute(context, vmId);
        verify(panoptaService, times(1)).removeServerMonitoring(anyLong(), anyString());
        verify(context, never()).execute(DeletePanoptaCustomer.class, shopperId);
    }

    @Test
    public void skipCustomerDeleteIfActiveServersStillExistInPanopta() {
        panoptaServerList.get(0).name = UUID.randomUUID().toString();
        when(panoptaDataService.getPanoptaServerDetailsList(shopperId)).thenReturn(Collections.emptyList());
        when(panoptaService.getActiveServers(shopperId)).thenReturn(panoptaServerList);
        when(panoptaService.getSuspendedServers(shopperId)).thenReturn(Collections.emptyList());
        command.execute(context, vmId);
        verify(context, never()).execute(DeletePanoptaCustomer.class, shopperId);
    }

    @Test
    public void skipCustomerDeleteIfSuspendedServersStillExistInPanopta() {
        panoptaServerList.get(0).name = UUID.randomUUID().toString();
        when(panoptaDataService.getPanoptaServerDetailsList(shopperId)).thenReturn(Collections.emptyList());
        when(panoptaService.getActiveServers(shopperId)).thenReturn(Collections.emptyList());
        when(panoptaService.getSuspendedServers(shopperId)).thenReturn(panoptaServerList);
        command.execute(context, vmId);
        verify(context, never()).execute(DeletePanoptaCustomer.class, shopperId);
    }
}
