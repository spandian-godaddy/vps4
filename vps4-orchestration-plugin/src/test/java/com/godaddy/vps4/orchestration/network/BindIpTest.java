package com.godaddy.vps4.orchestration.network;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.godaddy.vps4.orchestration.TestCommandContext;
import com.godaddy.vps4.orchestration.hfs.network.BindIp;
import com.godaddy.vps4.orchestration.hfs.network.WaitForAddressAction;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.GuiceCommandProvider;
import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;

public class BindIpTest {

    NetworkService networkService = mock(NetworkService.class);
    WaitForAddressAction waitAction = mock(WaitForAddressAction.class);

    BindIp command = new BindIp(networkService);

    Injector injector = Guice.createInjector(binder -> {
        binder.bind(WaitForAddressAction.class).toInstance(waitAction);
    });
    CommandContext context = spy(new TestCommandContext(new GuiceCommandProvider(injector)));

    @Test
    public void testExecuteWithActionSuccess() {
        BindIp.BindIpRequest request = new BindIp.BindIpRequest();
        request.addressId = 42;
        request.vmId = 23;
        boolean shouldForce = false;

        AddressAction waitingHfsAction = mock(AddressAction.class);
        when(networkService.bindIp(request.addressId, request.vmId, shouldForce)).thenReturn(waitingHfsAction);

        assertNull(command.execute(context, request));
        verify(networkService, times(1)).bindIp(request.addressId, request.vmId, shouldForce);
        verify(context, times(1)).execute(WaitForAddressAction.class, waitingHfsAction);
    }

}
