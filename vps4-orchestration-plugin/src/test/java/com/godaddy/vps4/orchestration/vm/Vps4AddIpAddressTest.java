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
import junit.framework.Assert;

public class Vps4AddIpAddressTest {
    ActionService actionService = mock(ActionService.class);
    VmService vmService = mock(VmService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    NetworkService networkService = mock(NetworkService.class);
    AllocateIp allocateIp = mock(AllocateIp.class);

    Vps4AddIpAddress command = new Vps4AddIpAddress(actionService, vmService, virtualMachineService, networkService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(Vps4AddIpAddress.class);
        binder.bind(AllocateIp.class).toInstance(allocateIp);
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(NetworkService.class).toInstance(networkService);

        //binder.bind(SysAdminService.class).toInstance(sysAdminService);
    });

    CommandContext context = mock(CommandContext.class);

    @Test
    public void testAddIpAllocates() {
        Vps4AddIpAddress.Request request = new Vps4AddIpAddress.Request();

        ServerType vmServerType = new ServerType();
        vmServerType.platform = ServerType.Platform.OVH;
        vmServerType.serverType = ServerType.Type.VIRTUAL;
        ServerSpec vmSpec = new ServerSpec();
        vmSpec.serverType = vmServerType;
        VirtualMachine virtualMachine = new VirtualMachine(UUID.randomUUID(),
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
                                                           "fake.hostname.com",
                                                           0,
                                                           UUID.randomUUID(),
                                                           null);
        request.vmId = virtualMachine.vmId;
        request.zone = "vps4-phx3";
        request.sgid = "vps4-unittest-1234";

        IpAddress hfsAddress = new IpAddress();
        hfsAddress.address = "1.2.3.4";
        hfsAddress.addressId = 3425;
        when(virtualMachineService.getVirtualMachine(any())).thenReturn(virtualMachine);
        when(context.execute(eq(AllocateIp.class), any(AllocateIp.Request.class))).thenReturn(hfsAddress);

        try{
            command.executeWithAction(context, request);
        }catch(Exception e){
            System.out.println(e);
            Assert.fail();
        }
        verify(context, times(1)).execute(eq(AllocateIp.class), any(AllocateIp.Request.class));
    }


}
