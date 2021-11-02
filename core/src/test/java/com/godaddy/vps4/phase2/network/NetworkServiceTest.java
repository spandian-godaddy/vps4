package com.godaddy.vps4.phase2.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.phase2.SqlTestData;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class NetworkServiceTest {

    private NetworkService networkService;
    ProjectService projectService;
    private Injector injector = Guice.createInjector(new DatabaseModule());

    private UUID orionGuid = UUID.randomUUID();
    private UUID orionGuidTwo = UUID.randomUUID();
    private DataSource dataSource;
    private VirtualMachine vm;
    private VirtualMachine vmTwo;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);
        networkService = new JdbcNetworkService(dataSource);
        projectService = new JdbcProjectService(dataSource);

        vm = SqlTestData.insertTestVm(orionGuid, dataSource);
        vmTwo = SqlTestData.insertTestVm(orionGuidTwo, dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vm.vmId, dataSource);
        SqlTestData.cleanupTestVmAndRelatedData(vmTwo.vmId, dataSource);
    }

    @Test
    public void testCreateIp() {

        long hfsAddressId = 123;
        String ipAddress = "127.0.0.1";

        long primaryId = 125;
        String primaryAddress = "192.168.1.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        IpAddress address = networkService.createIpAddress(hfsAddressId, vm.vmId, ipAddress, IpAddress.IpAddressType.SECONDARY);

        List<IpAddress> ips = networkService.getVmIpAddresses(vm.vmId);
        assertEquals(2, ips.size());

        IpAddress ip = networkService.getIpAddress(address.addressId);

        assertEquals(vm.vmId, ip.vmId);
        assertTrue(ip.validUntil.isAfter(Instant.now()));
        assertNotNull(ip.validOn);
        assertEquals(hfsAddressId, ip.hfsAddressId);
        assertEquals(ipAddress, ip.ipAddress);
        assertEquals(IpAddress.IpAddressType.SECONDARY, ip.ipAddressType);

        IpAddress primary = networkService.getVmPrimaryAddress(vm.vmId);

        assertEquals(vm.vmId, primary.vmId);
        assertTrue(primary.validUntil.isAfter(Instant.now()));
        assertNotNull(primary.validOn);
        assertEquals(primaryId, primary.hfsAddressId);
        assertEquals(primaryAddress, primary.ipAddress);
        assertEquals(IpAddress.IpAddressType.PRIMARY, primary.ipAddressType);
    }

    @Test
    public void testDeleteIp() {
        long hfsAddressId = 123;
        String ipAddress = "127.0.0.1";
        IpAddress address = networkService.createIpAddress(hfsAddressId, vm.vmId, ipAddress, IpAddress.IpAddressType.SECONDARY);

        IpAddress ip = networkService.getIpAddress(address.addressId);

        networkService.destroyIpAddress(address.addressId);

        IpAddress deletedIp = networkService.getIpAddress(address.addressId);
        assertTrue(deletedIp.validUntil.isBefore(ip.validUntil));
    }

    @Test
    public void testDuplicatePrimaryFails() {
        long primaryId = 125;
        String primaryAddress = "192.168.1.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        try {
            networkService.createIpAddress(primaryId + 1, vm.vmId, "127.0.0.2", IpAddress.IpAddressType.PRIMARY);
            Assert.fail("This should fail to insert a new Primary IP");
        } catch (Exception se) {
        }
    }

    @Test
    public void testDuplicateIpAddress() {
        long addressId = 123;
        long primaryId = 125;
        String primaryAddress = "192.168.1.1";
        String ipAddress = "127.0.0.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        networkService.createIpAddress(addressId, vm.vmId, ipAddress, IpAddress.IpAddressType.SECONDARY);

        try {
            networkService.createIpAddress(126, vm.vmId, primaryAddress, IpAddress.IpAddressType.SECONDARY);
            Assert.fail("This should fail to insert a duplicate IP address");
        } catch (Exception e) {
        }
    }

    @Test
    public void testReuseOfPrimaryIp() {
        long hfsAddressId = 125;
        String primaryAddress = "192.168.1.1";

        IpAddress address = networkService.createIpAddress(hfsAddressId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        networkService.destroyIpAddress(address.addressId);

        networkService.createIpAddress(hfsAddressId + 1, vmTwo.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
    }

    @Test(expected = RuntimeException.class)
    public void testReuseOfActiveIpFails() {
        long primaryId = 125;
        String primaryAddress = "192.168.1.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        // Should fail to insert a duplicate active IP address
        networkService.createIpAddress(primaryId + 1, vmTwo.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
    }

    @Test
    public void testGetSecondaryIpsOnly() {
        long primaryId = 125;
        String primaryAddress = "192.168.1.1";
        String ipAddress = "127.0.0.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        networkService.createIpAddress(126, vm.vmId, ipAddress, IpAddress.IpAddressType.SECONDARY);
        List<IpAddress> ips = networkService.getVmSecondaryAddress(vm.hfsVmId);
        assertEquals(1, ips.size());
    }

    @Test
    public void testGetValidSecondaryIpsOnly() {
        String invalidIpAddress = "192.168.1.1";
        String ipAddress = "127.0.0.1";

        IpAddress address = networkService.createIpAddress(125, vm.vmId, invalidIpAddress, IpAddress.IpAddressType.SECONDARY);
        networkService.destroyIpAddress(address.addressId);

        networkService.createIpAddress(126, vm.vmId, ipAddress, IpAddress.IpAddressType.SECONDARY);
        List<IpAddress> ips = networkService.getVmSecondaryAddress(vm.hfsVmId);
        assertEquals(1, ips.size());
    }

    @Test
    public void testGetIpAddressesIPV4s() {
        networkService.createIpAddress(126, vm.vmId, "192.168.1.1", IpAddress.IpAddressType.SECONDARY);
        networkService.createIpAddress(127, vm.vmId, "127.0.0.1", IpAddress.IpAddressType.SECONDARY);

        networkService.createIpAddress(126, vm.vmId, "2001:0db8:85a3:0000:0000:8a2e:0370:7334", IpAddress.IpAddressType.SECONDARY);

        List<IpAddress> ips = networkService.getActiveIpAddresses(vm.hfsVmId, 4);
        assertEquals(2, ips.size());
    }

    @Test
    public void testGetIpAddressesIPV6s() {
        networkService.createIpAddress(126, vm.vmId, "192.168.1.1", IpAddress.IpAddressType.SECONDARY);
        networkService.createIpAddress(127, vm.vmId, "127.0.0.1", IpAddress.IpAddressType.SECONDARY);

        networkService.createIpAddress(126, vm.vmId, "2001:0db8:85a3:0000:0000:8a2e:0370:7334", IpAddress.IpAddressType.SECONDARY);

        List<IpAddress> ips = networkService.getActiveIpAddresses(vm.hfsVmId, 6);
        assertEquals(1, ips.size());
    }
}
