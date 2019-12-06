package com.godaddy.vps4.orchestration.monitoring;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveMonitoringTest {

    NetworkService vps4NetworkService = mock(NetworkService.class);
    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    PanoptaService panoptaService = mock(PanoptaService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    CreditService creditService = mock(CreditService.class);
    VirtualMachine virtualMachine = mock(VirtualMachine.class);
    VirtualMachineCredit credit = mock(VirtualMachineCredit.class);
    CommandContext context = mock(CommandContext.class);
    IpAddress primaryIp = mock(IpAddress.class);
    PanoptaDetail panoptaDetails = mock(PanoptaDetail.class);
    Long pingCheckId = 23L;
    UUID vmId = UUID.randomUUID();
    String shopperId = "fake-shopper-id";

    Vps4RemoveMonitoring command =
            new Vps4RemoveMonitoring(vps4NetworkService, panoptaDataService, panoptaService, creditService,
                                     virtualMachineService);

    @Before
    public void setUp() {
        primaryIp.pingCheckId = pingCheckId;
        when(vps4NetworkService.getVmPrimaryAddress(vmId)).thenReturn(primaryIp);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetails);
        when(panoptaDataService.checkAndSetPanoptaCustomerDestroyed(shopperId)).thenReturn(true);
        when(virtualMachineService.getVirtualMachine(vmId)).thenReturn(virtualMachine);
        when(creditService.getVirtualMachineCredit(any())).thenReturn(credit);
        when(credit.getShopperId()).thenReturn(shopperId);
    }

    @Test
    public void executesRemoveNodePingMonitoring() {
        command.execute(context, vmId);
        verify(context).execute(RemoveNodePingMonitoring.class, primaryIp);
    }

    @Test
    public void skipsRemoveNodePingIfNullIp() {
        when(vps4NetworkService.getVmPrimaryAddress(vmId)).thenReturn(null);
        command.execute(context, vmId);
        verify(context, never()).execute(eq(RemoveNodePingMonitoring.class), any());
    }

    @Test
    public void skipsRemoveNodePingIfNullPingCheckId() {
        primaryIp.pingCheckId = null;
        command.execute(context, vmId);
        verify(context, never()).execute(RemoveNodePingMonitoring.class, primaryIp);
    }

    @Test
    public void executesRemovePanoptaMonitoring() {
        command.execute(context, vmId);
        verify(context).execute(RemovePanoptaMonitoring.class, vmId);
        verify(panoptaDataService).setPanoptaServerDestroyed(vmId);
        verify(context).execute(DeletePanoptaCustomer.class, shopperId);
    }

    @Test
    public void skipsRemovePanoptaIfNullPanoptaDetail() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        command.execute(context, vmId);
        verify(context, never()).execute(RemovePanoptaMonitoring.class, vmId);
        verify(panoptaDataService, never()).setPanoptaServerDestroyed(vmId);
        verify(context, never()).execute(DeletePanoptaCustomer.class, shopperId);
    }

    @Test
    public void skipPanotaCustomerDeleteIfActiveServers() {
        List<PanoptaServerDetails> panoptaServerDetailsList =
                Arrays.asList(mock(PanoptaServerDetails.class), mock(PanoptaServerDetails.class));
        when(panoptaDataService.getActivePanoptaServers(shopperId)).thenReturn(panoptaServerDetailsList);
        command.execute(context, vmId);
        verify(context).execute(RemovePanoptaMonitoring.class, vmId);
        verify(panoptaDataService).setPanoptaServerDestroyed(vmId);
        verify(context, never()).execute(DeletePanoptaCustomer.class, shopperId);
    }

    @Test
    public void skipPanotaCustomerDeleteIfCustomerHasActiveServersInPanopta() {
        List<PanoptaServerDetails> panoptaServerDetailsList =
                Arrays.asList(mock(PanoptaServerDetails.class), mock(PanoptaServerDetails.class));
        List<PanoptaServer> panoptaServerList = Collections.singletonList(mock(PanoptaServer.class));
        when(panoptaDataService.getActivePanoptaServers(shopperId)).thenReturn(null);
        when(panoptaService.getActiveServers(eq(shopperId))).thenReturn(panoptaServerList);
        command.execute(context, vmId);
        verify(context).execute(RemovePanoptaMonitoring.class, vmId);
        verify(panoptaDataService).setPanoptaServerDestroyed(vmId);
        verify(context, never()).execute(DeletePanoptaCustomer.class, shopperId);
    }
}
