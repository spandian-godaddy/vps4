package com.godaddy.vps4.phase2;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.vm.AgentDetails;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.util.TroubleshootVmService;
import com.godaddy.vps4.vm.TroubleshootInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.util.DefaultTroubleshootVmService;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmTroubleshootResource;

import junit.framework.Assert;

public class VmTroubleshootResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private VmService hfsVmService = mock(VmService.class);

    private UUID vmId = UUID.randomUUID();
    private long hfsVmId = 23L;
    private String ip = "10.1.2.3";
    private AgentDetails agentDetails;
    private VmTroubleshootResource vmTroubleResource;

    private Socket socketMock = mock(Socket.class);
    private InetAddress inetAddrMock = mock(InetAddress.class);
    private TroubleshootVmService troubleHelper = new DefaultTroubleshootVmService(hfsVmService) {
        @Override
        public Socket createSocket() {
            return socketMock;
        }

        @Override
        public InetAddress getInetAddress(String ipAddress) throws IOException {
            return inetAddrMock;
        }
    };

    @Before
    public void setupTest() throws IOException {
        agentDetails = new AgentDetails();
        agentDetails.agentStatus = "OK";

        VirtualMachine vm = mock(VirtualMachine.class);
        vm.hfsVmId = hfsVmId;
        vm.primaryIpAddress = mock(IpAddress.class);
        vm.primaryIpAddress.ipAddress = ip;
        when(vmResource.getVm(vmId)).thenReturn(vm);
        when(hfsVmService.getHfsAgentDetails(vm.hfsVmId)).thenReturn(agentDetails);
        when(inetAddrMock.isReachable(anyInt())).thenReturn(true);

        vmTroubleResource = new VmTroubleshootResource(vmResource, troubleHelper);
    }

    @Test
    public void testTroubleshootVmIsOk() {
        TroubleshootInfo info = vmTroubleResource.troubleshootVm(vmId);
        Assert.assertEquals(true, info.isOk());
        Assert.assertEquals(true, info.status.canPing);
        Assert.assertEquals(true, info.status.isPortOpen2224);
        Assert.assertEquals("OK", info.status.hfsAgentStatus);
    }

    @Test
    public void testVmUnreachable() throws IOException {
        when(inetAddrMock.isReachable(anyInt())).thenThrow(new IOException());

        TroubleshootInfo info = vmTroubleResource.troubleshootVm(vmId);
        Assert.assertEquals(false, info.isOk());
        Assert.assertEquals(false, info.status.canPing);
    }

    @Test
    public void testHfsVmPortBlocked() throws IOException {
        when(inetAddrMock.isReachable(anyInt())).thenReturn(true);
        doThrow(new IOException()).when(socketMock).connect(any(), anyInt());

        TroubleshootInfo info = vmTroubleResource.troubleshootVm(vmId);
        Assert.assertEquals(false, info.isOk());
        Assert.assertEquals(false, info.status.isPortOpen2224);
    }

    @Test
    public void testAgentStatusError() throws IOException {
        agentDetails.agentStatus = "NOT_RESPONDING";
        TroubleshootInfo info = vmTroubleResource.troubleshootVm(vmId);
        Assert.assertEquals(false, info.isOk());
        Assert.assertEquals("NOT_RESPONDING", info.status.hfsAgentStatus);
    }

}
