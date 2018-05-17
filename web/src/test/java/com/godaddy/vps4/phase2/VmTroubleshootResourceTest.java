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

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.vm.TroubleshootInfo;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.util.TroubleshootVmHelper;
import com.godaddy.vps4.web.vm.VmResource;
import com.godaddy.vps4.web.vm.VmTroubleshootResource;

import junit.framework.Assert;

public class VmTroubleshootResourceTest {

    private VmResource vmResource = mock(VmResource.class);
    private UUID vmId = UUID.randomUUID();
    private String ip = "10.1.2.3";
    private VmTroubleshootResource vmTroubleResource;

    private Socket socketMock = mock(Socket.class);
    private InetAddress inetAddrMock = mock(InetAddress.class);
    private TroubleshootVmHelper troubleHelper = new TroubleshootVmHelper() {
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
    public void setupTest() {
        VirtualMachine vm = mock(VirtualMachine.class);
        vm.primaryIpAddress = mock(IpAddress.class);
        vm.primaryIpAddress.ipAddress = ip;
        when(vmResource.getVm(vmId)).thenReturn(vm);
        vmTroubleResource = new VmTroubleshootResource(vmResource, troubleHelper);
    }

    @Test
    public void testTroubleshootVmIsOk() throws IOException {
        when(inetAddrMock.isReachable(anyInt())).thenReturn(true);

        TroubleshootInfo info = vmTroubleResource.troubleshootVm(vmId);
        Assert.assertEquals(true, info.isOk());
        Assert.assertEquals(true, info.status.canPing);
        Assert.assertEquals(true, info.status.isPortOpen2223);
        Assert.assertEquals(true, info.status.isPortOpen2224);
    }

    @Test
    public void testTroubleshootVmUnreachable() throws IOException {
        when(inetAddrMock.isReachable(anyInt())).thenThrow(new IOException());

        TroubleshootInfo info = vmTroubleResource.troubleshootVm(vmId);
        Assert.assertEquals(false, info.isOk());
        Assert.assertEquals(false, info.status.canPing);
    }

    @Test
    public void testTroubleshootVmPortBlocked() throws IOException {
        when(inetAddrMock.isReachable(anyInt())).thenReturn(true);
        doThrow(new IOException()).when(socketMock).connect(any(), anyInt());

        TroubleshootInfo info = vmTroubleResource.troubleshootVm(vmId);
        Assert.assertEquals(false, info.isOk());
        Assert.assertEquals(true, info.status.canPing);
        Assert.assertEquals(false, info.status.isPortOpen2223);
        Assert.assertEquals(false, info.status.isPortOpen2224);
    }

}
