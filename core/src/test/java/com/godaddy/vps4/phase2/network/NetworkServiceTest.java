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

        long addressId = 123;
        String ipAddress = "127.0.0.1";

        long primaryId = 125;
        String primaryAddress = "192.168.1.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        networkService.createIpAddress(addressId, vm.vmId, ipAddress, IpAddress.IpAddressType.SECONDARY);

        List<IpAddress> ips = networkService.getVmIpAddresses(vm.vmId);
        assertEquals(2, ips.size());

        IpAddress ip = networkService.getIpAddress(addressId);

        assertEquals(vm.vmId, ip.vmId);
        assertTrue(ip.validUntil.isAfter(Instant.now()));
        assertNotNull(ip.validOn);
        assertEquals(addressId, ip.ipAddressId);
        assertEquals(ipAddress, ip.ipAddress);
        assertEquals(IpAddress.IpAddressType.SECONDARY, ip.ipAddressType);

        IpAddress primary = networkService.getVmPrimaryAddress(vm.vmId);

        assertEquals(vm.vmId, primary.vmId);
        assertTrue(primary.validUntil.isAfter(Instant.now()));
        assertNotNull(primary.validOn);
        assertEquals(primaryId, primary.ipAddressId);
        assertEquals(primaryAddress, primary.ipAddress);
        assertEquals(IpAddress.IpAddressType.PRIMARY, primary.ipAddressType);
    }

    @Test
    public void testDeleteIp() {
        long addressId = 123;
        String ipAddress = "127.0.0.1";
        networkService.createIpAddress(addressId, vm.vmId, ipAddress, IpAddress.IpAddressType.SECONDARY);

        IpAddress ip = networkService.getIpAddress(addressId);

        networkService.destroyIpAddress(addressId);

        IpAddress deletedIp = networkService.getIpAddress(addressId);
        assertTrue(deletedIp.validUntil.isBefore(ip.validUntil));
    }

    @Test
    public void TestDuplicatePrimaryFails() {
        long primaryId = 125;
        String primaryAddress = "192.168.1.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        try {
            networkService.createIpAddress(primaryId + 1, vm.vmId, "127.0.0.2", IpAddress.IpAddressType.PRIMARY);
            Assert.fail("This should fail to insert a new Primary IP");
        }
        catch (Exception se) {
        }
    }

    @Test
    public void TestDuplicateIpAddress() {
        long addressId = 123;
        long primaryId = 125;
        String primaryAddress = "192.168.1.1";
        String ipAddress = "127.0.0.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        networkService.createIpAddress(addressId, vm.vmId, ipAddress, IpAddress.IpAddressType.SECONDARY);

        try {
            networkService.createIpAddress(126, vm.vmId, primaryAddress, IpAddress.IpAddressType.SECONDARY);
            Assert.fail("This should fail to insert a duplicate IP address");
        }
        catch (Exception e) {
        }
    }

    @Test
    public void TestReuseOfPrimaryIp() {
        long primaryId = 125;
        String primaryAddress = "192.168.1.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        networkService.destroyIpAddress(primaryId);

        networkService.createIpAddress(primaryId + 1, vmTwo.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
    }

    @Test(expected=RuntimeException.class)
    public void TestReuseOfActiveIpFails() {
        long primaryId = 125;
        String primaryAddress = "192.168.1.1";

        networkService.createIpAddress(primaryId, vm.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
        // Should fail to insert a duplicate active IP address
        networkService.createIpAddress(primaryId + 1, vmTwo.vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);
    }
}
