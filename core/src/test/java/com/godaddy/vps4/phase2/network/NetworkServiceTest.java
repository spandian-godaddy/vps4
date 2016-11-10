package com.godaddy.vps4.phase2.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.jdbc.JdbcVirtualMachineService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class NetworkServiceTest {

    private NetworkService networkService;
    private VirtualMachineService virtualMachineService;
    ProjectService projectService;
    private Injector injector = Guice.createInjector(new DatabaseModule());

    private long project;
    private long vmId;
    private UUID orionGuid = UUID.randomUUID();
    private DataSource dataSource;

    @Before
    public void setupService() {
        dataSource = injector.getInstance(DataSource.class);

        networkService = new JdbcNetworkService(dataSource);
        projectService = new JdbcProjectService(dataSource);

        String projectName = "testNetwork";
        projectService.createProject(projectName, 1, 1);

        project = Sql.with(dataSource).exec("SELECT project_id FROM project WHERE project_name = ?",
                Sql.nextOrNull(rs -> rs.getLong("project_id")), projectName);
        vmId = Sql.with(dataSource).exec("SELECT max(vm_id) FROM virtual_machine",
                Sql.nextOrNull(this::mapVmId));

        virtualMachineService = new JdbcVirtualMachineService(dataSource);
        virtualMachineService.createVirtualMachineRequest(orionGuid, "linux", "none", 10, 0);
        virtualMachineService.provisionVirtualMachine(vmId, orionGuid, "networkTestVm", project, 1, 0, 1);
    }

    private long mapVmId(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return rs.getLong("vm_id");
        }
        return 0;
    }

    @After
    public void cleanup() {
        Sql.with(dataSource).exec("DELETE FROM ip_address WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine WHERE vm_id = ?", null, vmId);
        Sql.with(dataSource).exec("DELETE FROM virtual_machine_request WHERE orion_guid = ?", null, orionGuid);
        Sql.with(dataSource).exec("DELETE FROM user_project_privilege WHERE project_id = ?", null, project);
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_id = ?", null, project);
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
