package com.godaddy.vps4.network;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.web.network.AllocateIpWorker;

import gdg.hfs.vhfs.network.AddressAction;
import gdg.hfs.vhfs.network.IpAddress;
import gdg.hfs.vhfs.network.IpAddress.Status;
import gdg.hfs.vhfs.network.NetworkService;
import junit.framework.Assert;

public class AllocateIpWorkerTest {

    IpAddress ipAddress;
    AddressAction hfsAction;

    @Before
    public void setUpTestData() {

        hfsAction = new AddressAction();
        hfsAction.addressId = 12;
    }

    @Test
    public void testAllocateIp() {

        ipAddress = new IpAddress();
        ipAddress.address = "192.168.1.1";
        ipAddress.addressId = hfsAction.addressId;
        ipAddress.status = Status.UNBOUND;

        hfsAction.status = AddressAction.Status.COMPLETE;

        IpAddress ip = allocateWorkerTest("vps4-1");

        assertEquals(ipAddress.address, ip.address);
        assertEquals(ipAddress.addressId, ip.addressId);
    }

    private IpAddress allocateWorkerTest(String sgid) {

        Project project = new Project(1, "project1", "vps4-1", 1, Instant.now(), Instant.MAX);

        hfsAction.completedAt = Instant.now().toString();

        NetworkService networkService = Mockito.mock(NetworkService.class);
        Mockito.when(networkService.acquireIp(sgid)).thenReturn(hfsAction);
        Mockito.when(networkService.getAddressAction(hfsAction.addressId, hfsAction.addressActionId)).thenReturn(hfsAction);
        Mockito.when(networkService.getAddress(hfsAction.addressId)).thenReturn(ipAddress);

        com.godaddy.vps4.network.NetworkService vps4NetworkService = Mockito.mock(com.godaddy.vps4.network.NetworkService.class);

        AllocateIpWorker worker = new AllocateIpWorker(IpAddressType.PRIMARY, project, networkService, vps4NetworkService);
        IpAddress ip = worker.call();

        verify(networkService).acquireIp(sgid);
        return ip;
    }

    @Test
    public void testAllocateIpFailed() {
        hfsAction.status = AddressAction.Status.FAILED;
        try {
            allocateWorkerTest("vps4-1");
            Assert.fail("CommandException expected!");
        }
        catch (Vps4Exception ve) {
            assertEquals("ALLOCATE_IP_FAILED", ve.getId());
        }
    }

    @Test
    public void testAllocateIpNotUnbound() {

        ipAddress = new IpAddress();
        ipAddress.address = "192.168.1.1";
        ipAddress.addressId = hfsAction.addressId;
        ipAddress.status = Status.BOUND;

        hfsAction.status = AddressAction.Status.COMPLETE;

        try {
            allocateWorkerTest("vps4-1");
            Assert.fail("IllegalStateException should have been thrown for address not in UNBOUND status");
        }
        catch (Vps4Exception ve) {
            assertEquals("ALLOCATE_IP_FAILED", ve.getId());
        }
    }

}
