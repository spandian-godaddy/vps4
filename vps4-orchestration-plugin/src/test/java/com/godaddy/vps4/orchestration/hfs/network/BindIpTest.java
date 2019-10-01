package com.godaddy.vps4.orchestration.hfs.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
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

public class BindIpTest {

    NetworkServiceV2 networkService = mock(NetworkServiceV2.class);
    WaitForAddressAction addressWaitCmd = mock(WaitForAddressAction.class);
    UnbindIp unbindCmd = mock(UnbindIp.class);
    AddressAction waitingHfsAction = mock(AddressAction.class);

    BindIp command = new BindIp(networkService);
    BindIp.Request request;
    long addressId = 42L;
    long hfsVmId = 23L;

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForAddressAction.class).toInstance(addressWaitCmd);
        binder.bind(UnbindIp.class).toInstance(unbindCmd);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));


    @Before
    public void setUp() {
        request = new BindIp.Request();
        request.addressId = addressId;
        request.hfsVmId = hfsVmId;
        request.shouldForce = false;
        setupHfsAddress(Status.UNBOUND);

        when(networkService.bindIp(eq(request.addressId), eq(request.hfsVmId), anyBoolean())).thenReturn(waitingHfsAction);
    }

    private void setupHfsAddress(Status status) {
        setupHfsAddress(status, hfsVmId);
    }

    private void setupHfsAddress(Status status, long vmId) {
        IpAddress hfsAddress = new IpAddress();
        hfsAddress.status = status;
        hfsAddress.serverId = vmId;
        hfsAddress.addressId = addressId;
        when(networkService.getAddress(request.addressId)).thenReturn(hfsAddress);
    }

    @Test
    public void bindsHfsUnboundAddress() {
        assertNull(command.execute(context, request));
        verify(networkService).bindIp(request.addressId, request.hfsVmId, request.shouldForce);
        verify(context).execute(WaitForAddressAction.class, waitingHfsAction);
    }

    @Test(expected=IllegalStateException.class)
    public void invalidHfsAddressStatusForBindIp() {
        setupHfsAddress(Status.BINDING);
        command.execute(context, request);
    }

    @Test
    public void handlesHfsAddressAlreadyProperlyBound() {
        setupHfsAddress(Status.BOUND);
        command.execute(context, request);
        verify(networkService, never()).bindIp(request.addressId, request.hfsVmId, request.shouldForce);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void unforcedUnbindHfsAddressBoundToUnexpectedServer() {
        long unknownServerId = 13L;
        setupHfsAddress(Status.BOUND, unknownServerId);
        command.execute(context, request);
    }

    @Test
    public void forcedUnbindHfsAddressBoundToUnexpectedServer() {
        long unknownServerId = 13L;
        request.shouldForce = true;
        setupHfsAddress(Status.BOUND, unknownServerId);
        command.execute(context, request);

        ArgumentCaptor<UnbindIp.Request> argument = ArgumentCaptor.forClass(UnbindIp.Request.class);
        verify(context).execute(startsWith("ForceUnbindIP"), eq(UnbindIp.class), argument.capture());
        assertEquals(addressId, argument.getValue().addressId);
        assertTrue(argument.getValue().forceIfVmInaccessible);
        verify(networkService).bindIp(request.addressId, request.hfsVmId, request.shouldForce);
    }

}
