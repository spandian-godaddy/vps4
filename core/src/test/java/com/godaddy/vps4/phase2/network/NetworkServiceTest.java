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
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class NetworkServiceTest {

    private NetworkService networkService;
    private VirtualMachineService virtualMachineService;
    ProjectService projectService;
    private Injector injector = Guice.createInjector(new DatabaseModule());

    private long projectId;
    private long vmId;
    private UUID orionGuid = UUID.randomUUID();
    private DataSource dataSource;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);

        networkService = new JdbcNetworkService(dataSource);
        projectService = new JdbcProjectService(dataSource);

        projectId = projectService.createProject("testNetwork", 1, 1).getProjectId();

        vmId = SqlTestData.insertTestVm(orionGuid, projectId, dataSource);
    }

    @After
    public void cleanup() {
        SqlTestData.cleanupTestVmAndRelatedData(vmId, dataSource);
        SqlTestData.cleanupTestProject(projectId, dataSource);
    }

    @Test
    public void testService() {

        long addressId = 123;
        String ipAddress = "127.0.0.1";

        long primaryId = 125;
        String primaryAddress = "192.168.1.1";

        networkService.createIpAddress(addressId, vmId, ipAddress, IpAddress.IpAddressType.PRIMARY);
        networkService.createIpAddress(addressId + 1, vmId, "127.0.0.2", IpAddress.IpAddressType.SECONDARY);
        networkService.createIpAddress(primaryId, vmId, primaryAddress, IpAddress.IpAddressType.PRIMARY);

        List<IpAddress> ips = networkService.getVmIpAddresses(vmId);
        assertEquals(3, ips.size());

        IpAddress ip = networkService.getIpAddress(addressId);

        assertEquals(vmId, ip.vmId);
        assertTrue(ip.validUntil.isAfter(Instant.now()));
        assertNotNull(ip.validOn);
        assertEquals(addressId, ip.ipAddressId);
        assertEquals(ipAddress, ip.ipAddress);
        assertEquals(IpAddress.IpAddressType.SECONDARY, ip.ipAddressType);

        IpAddress primary = networkService.getVmPrimaryAddress(vmId);

        assertEquals(vmId, primary.vmId);
        assertTrue(primary.validUntil.isAfter(Instant.now()));
        assertNotNull(primary.validOn);
        assertEquals(primaryId, primary.ipAddressId);
        assertEquals(primaryAddress, primary.ipAddress);
        assertEquals(IpAddress.IpAddressType.PRIMARY, primary.ipAddressType);

        networkService.destroyIpAddress(addressId);

        IpAddress deletedIp = networkService.getIpAddress(addressId);
        assertTrue(deletedIp.validUntil.isBefore(ip.validUntil));
    }
}
