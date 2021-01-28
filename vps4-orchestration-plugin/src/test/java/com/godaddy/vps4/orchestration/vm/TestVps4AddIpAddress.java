package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.Test;

import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.vhfs.network.IpAddress;
import com.godaddy.hfs.vm.VmService;
import junit.framework.Assert;

public class TestVps4AddIpAddress {
    ActionService actionService = mock(ActionService.class);
    VmService vmService = mock(VmService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    NetworkService networkService = mock(NetworkService.class);
    AllocateIp allocateIp = mock(AllocateIp.class);
    BindIp bindIp = mock(BindIp.class);

    Vps4AddIpAddress command = new Vps4AddIpAddress(actionService, vmService, virtualMachineService, networkService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(Vps4AddIpAddress.class);
        binder.bind(AllocateIp.class).toInstance(allocateIp);
        binder.bind(BindIp.class).toInstance(bindIp);
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(NetworkService.class).toInstance(networkService);

        //binder.bind(SysAdminService.class).toInstance(sysAdminService);
    });

    CommandContext context = mock(CommandContext.class);

    @Test
    public void testAddIpAllocatesAndBinds() {
        Vps4AddIpAddress.Request request = new Vps4AddIpAddress.Request();
        VirtualMachine virtualMachine = new VirtualMachine(UUID.randomUUID(),
                1111, UUID.randomUUID(), 0, null, "fakeName", null, null,
                Instant.now(), null, null, null, "fake.hostname.com", 0, UUID.randomUUID());
        request.virtualMachine = virtualMachine;
        request.zone = "vps4-phx3";
        request.sgid = "vps4-unittest-1234";

        IpAddress ip = new IpAddress();
        ip.address = "1.2.3.4";
        ip.addressId = 3425;

        when(context.execute(eq(AllocateIp.class), any(AllocateIp.Request.class))).thenReturn(ip);

        try{
            command.executeWithAction(context, request);
        }catch(Exception e){
            System.out.println(e);
            Assert.fail();
        }
        verify(context, times(1)).execute(eq(AllocateIp.class), any(AllocateIp.Request.class));
        verify(context, times(1)).execute(eq(BindIp.class), any(BindIp.Request.class));
    }


}
