package com.godaddy.vps4.orchestration.monitoring;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.panopta.jdbc.PanoptaServerDetails;

import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveMonitoringTest {

    NetworkService vps4NetworkService = mock(NetworkService.class);
    CommandContext context = mock(CommandContext.class);
    IpAddress primaryIp = mock(IpAddress.class);
    Long pingCheckId = 23L;
    UUID vmId = UUID.randomUUID();
    List<PanoptaServerDetails> panoptaServerDetailsList;

    Vps4RemoveMonitoring command = new Vps4RemoveMonitoring(vps4NetworkService);

    @Before
    public void setUp() {
        primaryIp.pingCheckId = pingCheckId;
        panoptaServerDetailsList = Arrays.asList(mock(PanoptaServerDetails.class), mock(PanoptaServerDetails.class));
        when(vps4NetworkService.getVmPrimaryAddress(vmId)).thenReturn(primaryIp);
    }

    @Test
    public void executesRemovePanoptaMonitoring() {
        command.execute(context, vmId);
        verify(context).execute(RemovePanoptaMonitoring.class, vmId);
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
}
