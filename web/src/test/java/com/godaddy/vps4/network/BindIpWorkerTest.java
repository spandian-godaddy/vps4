package com.godaddy.vps4.network;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.web.network.BindIpAction;
import com.godaddy.vps4.web.network.BindIpWorker;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.NetworkService;
import junit.framework.Assert;

public class BindIpWorkerTest {

    NetworkService networkService;
    com.godaddy.vps4.network.NetworkService vps4NetworkService;
    long addressId = 1;
    long vmId = 1;

    @Before
    public void setUpRequest() {
        networkService = Mockito.mock(NetworkService.class);
        vps4NetworkService = Mockito.mock(com.godaddy.vps4.network.NetworkService.class);
    }

    @Test
    public void testBindIp() throws Exception {

        bindWorkerTest(AddressAction.Status.COMPLETE);

        AddressAction actualAction = networkService.getAddressAction(addressId, vmId);
        assertEquals(AddressAction.Status.COMPLETE, actualAction.status);
    }

    private void bindWorkerTest(AddressAction.Status desiredStatus) throws Exception {

        AddressAction action = new AddressAction();
        action.addressActionId = 1;
        action.addressId = 1;
        action.completedAt = Instant.now().toString();
        action.status = desiredStatus;

        Mockito.when(networkService.bindIp(addressId, vmId)).thenReturn(action);
        Mockito.when(networkService.getAddressAction(addressId, vmId)).thenReturn(action);

        BindIpAction bindIpAction = new BindIpAction(addressId, "127.0.0.1", vmId, IpAddress.IpAddressType.PRIMARY);
        BindIpWorker bindIp = new BindIpWorker(bindIpAction, networkService, vps4NetworkService);
        bindIp.run();

        verify(networkService).bindIp(addressId, vmId);
    }

    @Test
    public void testBindIpFailed() throws Exception {
        try {
            bindWorkerTest(AddressAction.Status.FAILED);
            Assert.fail("CommandException expected!");
        }
        catch (Vps4Exception ve) {
            assertEquals("BIND_IP_FAILED", ve.getId());
        }
    }
}
