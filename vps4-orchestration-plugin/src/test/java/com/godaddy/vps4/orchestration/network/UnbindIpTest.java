package com.godaddy.vps4.orchestration.network;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.Test;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.IpAddress.Status;
import gdg.hfs.vhfs.network.NetworkService;

public class UnbindIpTest {
    NetworkService networkService = mock(NetworkService.class);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(gdg.hfs.vhfs.network.NetworkService.class).toInstance(networkService);
    });

    CommandContext context = new TestCommandContext(new GuiceCommandProvider(injector));

    @Test
    public void unbindIpAlreadyUnboudTest() {
        IpAddress ipAddress = new IpAddress();
        ipAddress.status = Status.UNBOUND;
        ipAddress.addressId = 1L;

        when(networkService.getAddress(ipAddress.addressId)).thenReturn(ipAddress);

        UnbindIp command = new UnbindIp(networkService);
        UnbindIp.Request request = new UnbindIp.Request();
        request.addressId = ipAddress.addressId;
        request.forceIfVmInaccessible = true;
        command.execute(context, request);

        verify(networkService, never()).unbindIp(ipAddress.addressId, request.forceIfVmInaccessible);
    }
}
