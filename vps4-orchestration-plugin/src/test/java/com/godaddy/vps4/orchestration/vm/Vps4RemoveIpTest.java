package com.godaddy.vps4.orchestration.vm;

import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;

import gdg.hfs.orchestration.CommandContext;

public class Vps4RemoveIpTest {
    CommandContext context = mock(CommandContext.class);
    Vps4RemoveIp command = new Vps4RemoveIp();

    @Test
    public void testExecuteDestroyPrimaryIp(){
        IpAddress ipAddress = new IpAddress();
        ipAddress.ipAddress = "1.2.3.4";
        ipAddress.ipAddressId = 3425;
        ipAddress.ipAddressType =  IpAddress.IpAddressType.PRIMARY;

        command.execute(context, ipAddress);
        verify(context, times(1)).execute(eq(ReleaseIp.class), eq(ipAddress.ipAddressId));
        verify(context, times(1)).execute(eq(UnbindIp.class), any(UnbindIp.Request.class));
        verify(context, never()).execute(eq(SetMailRelayQuota.class), any());
    }
    @Test
    public void testExecuteDestroySecondaryIp(){
        IpAddress ipAddress = new IpAddress();
        ipAddress.ipAddress = "1.2.3.4";
        ipAddress.ipAddressId = 3425;
        ipAddress.ipAddressType =  IpAddress.IpAddressType.SECONDARY;

        command.execute(context, ipAddress);
        verify(context, times(1)).execute(eq(ReleaseIp.class), eq(ipAddress.ipAddressId));
        verify(context, never()).execute(eq(UnbindIp.class), any(UnbindIp.Request.class));
        verify(context, never()).execute(eq(SetMailRelayQuota.class), any());
    }
}
