package com.godaddy.vps4.orchestration.hfs.network;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.UnbindIp;
import com.godaddy.vps4.orchestration.hfs.network.WaitForAddressAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.IpAddress.Status;
import gdg.hfs.vhfs.network.NetworkServiceV2;

public class UnbindIpTest {
    NetworkServiceV2 networkService = mock(NetworkServiceV2.class);
    WaitForAddressAction addressWaitCmd = mock(WaitForAddressAction.class);
    AddressAction waitingHfsAction = mock(AddressAction.class);

    UnbindIp command = new UnbindIp(networkService);
    UnbindIp.Request request;
    long addressId = 42L;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForAddressAction.class).toInstance(addressWaitCmd);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Before
    public void setUp() {
        request = new UnbindIp.Request();
        request.addressId = addressId;
        request.forceIfVmInaccessible = false;
        setupHfsAddress(Status.BOUND);

        when(networkService.unbindIp(eq(request.addressId), anyBoolean())).thenReturn(waitingHfsAction);
    }

    private void setupHfsAddress(Status status) {
        IpAddress hfsAddress = new IpAddress();
        hfsAddress.status = status;
        hfsAddress.addressId = addressId;
        when(networkService.getAddress(request.addressId)).thenReturn(hfsAddress);
    }

    @Test
    public void unbindsHfsBoundAddress() {
        assertNull(command.execute(context, request));
        verify(networkService).unbindIp(request.addressId, request.forceIfVmInaccessible);
        verify(context).execute(WaitForAddressAction.class, waitingHfsAction);
    }

    @Test
    public void unbindHfsAddressAlreadyUnbound() {
        setupHfsAddress(Status.UNBOUND);
        command.execute(context, request);
        verify(networkService, never()).unbindIp(request.addressId, request.forceIfVmInaccessible);
    }

    @Test
    public void unbindUnnecessaryForHfsAddressStatus() {
        setupHfsAddress(Status.RELEASED);
        command.execute(context, request);
        verify(networkService, never()).unbindIp(request.addressId, request.forceIfVmInaccessible);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void unforcedUnbindHfsAddressTranstionalStatus() {
        setupHfsAddress(Status.BINDING);
        command.execute(context, request);
    }

    @Test
    public void forcedUnbindHfsAddressTranstionalStatus() {
        request.forceIfVmInaccessible = true;
        setupHfsAddress(Status.UNBINDING);
        command.execute(context, request);
        verify(networkService).unbindIp(request.addressId, request.forceIfVmInaccessible);
    }

}
