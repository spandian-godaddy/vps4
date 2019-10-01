package com.godaddy.vps4.orchestration.hfs.network;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class ReleaseIpTest {

    NetworkServiceV2 networkService = mock(NetworkServiceV2.class);
    WaitForAddressAction waitAction = mock(WaitForAddressAction.class);
    AddressAction addressAction = mock(AddressAction.class);
    IpAddress hfsAddress = mock(IpAddress.class);
    Long addressId = 42L;

    ReleaseIp command = new ReleaseIp(networkService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForAddressAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setUp() {
        hfsAddress.status = IpAddress.Status.UNBOUND;
        when(networkService.getAddress(addressId)).thenReturn(hfsAddress);
        when(networkService.releaseIp(addressId)).thenReturn(addressAction);
    }

    @Test
    public void executesNetworkReleaseIp() {
        command.execute(context, addressId);
        verify(networkService).releaseIp(addressId);
    }

    @Test
    public void executesWaitForAddressAction() {
        command.execute(context, addressId);
        verify(context).execute(WaitForAddressAction.class, addressAction);
    }

    @Test
    public void doesNothingIfAddressAlreadyReleased() {
        hfsAddress.status = IpAddress.Status.RELEASED;
        command.execute(context, addressId);
        verify(networkService, never()).releaseIp(addressId);
    }

}
