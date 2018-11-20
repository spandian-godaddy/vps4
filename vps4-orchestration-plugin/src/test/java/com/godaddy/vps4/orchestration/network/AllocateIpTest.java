package com.godaddy.vps4.orchestration.network;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.AllocateIp;
import com.godaddy.vps4.orchestration.hfs.network.WaitForAddressAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkService;

public class AllocateIpTest {

    NetworkService networkService = mock(NetworkService.class);
    WaitForAddressAction waitAction = mock(WaitForAddressAction.class);

    AllocateIp command = new AllocateIp(networkService);
    AllocateIp.Request request = new AllocateIp.Request();
    AddressAction waitingHfsAction = mock(AddressAction.class);
    IpAddress ip = mock(IpAddress.class);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForAddressAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setUp() {
        request.sgid = "dummy-sgid";
        request.zone = "dummy-zone";

        waitingHfsAction.addressId = 42;

        ip.address = "10.1.2.3";
        ip.status = IpAddress.Status.UNBOUND;

        when(networkService.acquireIp(request.sgid, request.zone)).thenReturn(waitingHfsAction);
        when(networkService.getAddress(waitingHfsAction.addressId)).thenReturn(ip);
    }

    @Test
    public void testExecuteSuccess() {
        IpAddress result = command.execute(context, request);

        verify(networkService, times(1)).acquireIp(request.sgid, request.zone);
        verify(context, times(1)).execute(WaitForAddressAction.class, waitingHfsAction);
        verify(networkService, times(1)).getAddress(waitingHfsAction.addressId);
        assertEquals(result, ip);
    }

    @Test(expected = RuntimeException.class)
    public void testExecuteAddressIsNotUnbound() {
        ip.status = IpAddress.Status.ACQUIRING;

        command.execute(context, request);
    }

}
