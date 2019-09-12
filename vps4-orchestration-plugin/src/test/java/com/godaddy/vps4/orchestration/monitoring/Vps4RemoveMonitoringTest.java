package com.godaddy.vps4.orchestration.monitoring;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;

import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveMonitoringTest {

    NetworkService vps4NetworkService = mock(NetworkService.class);
    PanoptaDataService panoptaDataService = mock(PanoptaDataService.class);
    CommandContext context = mock(CommandContext.class);
    IpAddress primaryIp = mock(IpAddress.class);
    PanoptaDetail panoptaDetails = mock(PanoptaDetail.class);
    Long pingCheckId = 23L;
    UUID vmId = UUID.randomUUID();

    Vps4RemoveMonitoring command = new Vps4RemoveMonitoring(vps4NetworkService, panoptaDataService);

    @Before
    public void setUp() {
        primaryIp.pingCheckId = pingCheckId;
        when(vps4NetworkService.getVmPrimaryAddress(vmId)).thenReturn(primaryIp);
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(panoptaDetails);
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
        verify(panoptaDataService).setServerDestroyedInPanopta(vmId);
        verify(context).execute(DeletePanoptaCustomer.class, vmId);
    }

    @Test
    public void skipsRemovePanoptaIfNullPanoptaDetail() {
        when(panoptaDataService.getPanoptaDetails(vmId)).thenReturn(null);
        command.execute(context, vmId);
        verify(context, never()).execute(RemovePanoptaMonitoring.class, vmId);
        verify(panoptaDataService, never()).setServerDestroyedInPanopta(vmId);
        verify(context, never()).execute(DeletePanoptaCustomer.class, vmId);
    }
}
