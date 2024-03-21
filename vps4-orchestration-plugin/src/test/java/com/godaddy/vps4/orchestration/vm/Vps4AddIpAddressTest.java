package com.godaddy.vps4.orchestration.vm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import com.godaddy.vps4.orchestration.hfs.mailrelay.SetMailRelayQuota;
import com.godaddy.vps4.orchestration.hfs.network.ReleaseIp;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.ServerSpec;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.IpAddress;
import com.godaddy.hfs.vm.VmService;
import org.mockito.ArgumentCaptor;

public class Vps4AddIpAddressTest {
    ActionService actionService = mock(ActionService.class);
    VmService vmService = mock(VmService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    NetworkService networkService = mock(NetworkService.class);
    AllocateIp allocateIp = mock(AllocateIp.class);
    Vps4AddIpAddress.Request request;
    IpAddress hfsAddress;
    VirtualMachine virtualMachine;
    Vps4AddIpAddress command = new Vps4AddIpAddress(actionService, vmService, virtualMachineService, networkService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(Vps4AddIpAddress.class);
        binder.bind(AllocateIp.class).toInstance(allocateIp);
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(NetworkService.class).toInstance(networkService);
    });

    CommandContext context = mock(CommandContext.class);
    @Before
    public void setupTest() {
        request = new Vps4AddIpAddress.Request();
        ServerType vmServerType = new ServerType();
        vmServerType.serverType = ServerType.Type.VIRTUAL;
        ServerSpec vmSpec = new ServerSpec();
        vmSpec.serverType = vmServerType;
        virtualMachine = new VirtualMachine(UUID.randomUUID(),
                1111,
                UUID.randomUUID(),
                0,
                vmSpec,
                "fakeName",
                null,
                null,
                Instant.now(),
                null,
                null,
                null,
                null,
                "fake.hostname.com",
                0,
                UUID.randomUUID(),
                null,
                null);
        request.vmId = virtualMachine.vmId;
        request.zone = "vps4-phx3";
        request.sgid = "vps4-unittest-1234";
        request.internetProtocolVersion = 4;

        hfsAddress = new IpAddress();
        hfsAddress.address = "1.2.3.4";
        hfsAddress.addressId = 3425;
        when(virtualMachineService.getVirtualMachine(any())).thenReturn(virtualMachine);
        when(context.execute(eq(AllocateIp.class), any(AllocateIp.Request.class))).thenReturn(hfsAddress);
    }
    @Test
    public void testAddIpAllocates() throws Exception {
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(AllocateIp.class), any(AllocateIp.Request.class));
        verify(context, times(0)).execute(eq(ReleaseIp.class), any());
    }

    @Test
    public void testAddIpSetsMailRelayForIpv4VMs() throws Exception {
        ArgumentCaptor<SetMailRelayQuota.Request> captor = ArgumentCaptor.forClass(SetMailRelayQuota.Request.class);
        command.executeWithAction(context, request);
        verify(context, times(1)).execute(eq(SetMailRelayQuota.class), captor.capture());
        SetMailRelayQuota.Request req = captor.getValue();
        assertEquals(true, req.isAdditionalIp);
        assertEquals(hfsAddress.address, req.ipAddress);
        assertEquals(0, req.relays);
        assertEquals(0, req.quota);
    }

    @Test
    public void testAddIpDoesNOTSetMailRelayForIpv6() throws Exception {
        request.internetProtocolVersion = 6;
        command.executeWithAction(context, request);
        verify(context, times(0)).execute(eq(SetMailRelayQuota.class), any(SetMailRelayQuota.Request.class));
    }

    @Test
    public void testAddIpDoesNOTSetMailRelayForDed() throws Exception {
        ServerType dedServerType = new ServerType();
        dedServerType.serverType = ServerType.Type.DEDICATED;
        ServerSpec dedSpec = new ServerSpec();
        dedSpec.serverType = dedServerType;
        virtualMachine.spec = dedSpec;
        command.executeWithAction(context, request);
        verify(context, times(0)).execute(eq(SetMailRelayQuota.class), any(SetMailRelayQuota.Request.class));
    }
    @Test
    public void testAddIpReleasesIpIfDbFails() {
        when((context.execute(eq("Create-" + hfsAddress.addressId),
                any(Function.class), any()))).thenThrow(new RuntimeException());
        try {
            command.executeWithAction(context, request);
        } catch (Exception e) {
            verify(context, times(1)).execute(eq(AllocateIp.class), any(AllocateIp.Request.class));
            verify(context, times(1)).execute(eq(ReleaseIp.class), any());
        }
    }

}
