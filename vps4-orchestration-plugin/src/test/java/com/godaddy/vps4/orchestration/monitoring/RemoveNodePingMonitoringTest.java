package com.godaddy.vps4.orchestration.monitoring;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.util.MonitoringMeta;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.nodeping.NodePingService;

public class RemoveNodePingMonitoringTest {

    NodePingService hfsNodePingService = mock(NodePingService.class);
    MonitoringMeta monitoringMeta = mock(MonitoringMeta.class);
    CommandContext context = mock(CommandContext.class);
    IpAddress ip = mock(IpAddress.class);
    Long nodePingAccountId = 42L;
    Long pingCheckId = 23L;

    RemoveNodePingMonitoring command = new RemoveNodePingMonitoring(hfsNodePingService, monitoringMeta);

    @Before
    public void setUp() {
        ip.pingCheckId = pingCheckId;
        when(monitoringMeta.getAccountId()).thenReturn(nodePingAccountId);
    }

    @Test
    public void callsNodePingDeleteCheck() {
        assertNull(command.execute(context, ip));
        verify(hfsNodePingService).deleteCheck(nodePingAccountId, pingCheckId);
    }

    @Test
    public void ignoresNotFoundCheckIdError() {
        doThrow(new NotFoundException()).when(hfsNodePingService).deleteCheck(nodePingAccountId, pingCheckId);
        assertNull(command.execute(context, ip));
    }

}
