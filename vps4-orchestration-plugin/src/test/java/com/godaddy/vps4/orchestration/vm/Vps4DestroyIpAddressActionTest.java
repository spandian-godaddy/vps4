package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Test;

import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import junit.framework.Assert;

public class Vps4DestroyIpAddressActionTest {
    ActionService actionService = mock(ActionService.class);
    VmService vmService = mock(VmService.class);
    VirtualMachineService virtualMachineService = mock(VirtualMachineService.class);
    NetworkService networkService = mock(NetworkService.class);

    Vps4RemoveIp vps4DestroyIpAddress = mock(Vps4RemoveIp.class);

    Vps4DestroyIpAddressAction command = new Vps4DestroyIpAddressAction(actionService, vmService, virtualMachineService, networkService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(Vps4RemoveIp.class).toInstance(vps4DestroyIpAddress);
        binder.bind(ActionService.class).toInstance(actionService);
        binder.bind(VmService.class).toInstance(vmService);
        binder.bind(VirtualMachineService.class).toInstance(virtualMachineService);
        binder.bind(NetworkService.class).toInstance(networkService);
    });

    CommandContext context = mock(CommandContext.class);

    @Test
    public void testExecuteDestroyIpAddressAction() {
        Vps4DestroyIpAddressAction.Request request = new Vps4DestroyIpAddressAction.Request();
        VirtualMachine virtualMachine = new VirtualMachine(UUID.randomUUID(),
                                                           1111,
                                                           UUID.randomUUID(),
                                                           0,
                                                           null,
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
        request.addressId = 123;
        request.vmId = virtualMachine.vmId;

        IpAddress ip = new IpAddress(123, 3425, UUID.randomUUID(), "1.2.3.4", IpAddressType.SECONDARY, null, Instant.now(), null, 4);
        when(networkService.getIpAddress(ip.addressId)).thenReturn(ip);

        try{
            command.executeWithAction(context, request);
        }catch(Exception e){
            System.out.println(e);
            Assert.fail();
        }
        verify(context, times(1)).execute(Vps4RemoveIp.class, ip);

        verify(context, times(1)).execute(eq("MarkIpDeleted-123"), any(Function.class), eq(Void.class));
    }


}
