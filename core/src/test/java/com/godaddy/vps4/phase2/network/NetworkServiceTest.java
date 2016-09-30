package com.godaddy.vps4.phase2.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.network.jdbc.JdbcNetworkService;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class NetworkServiceTest {

    NetworkService networkService;
    Injector injector = Guice.createInjector(new DatabaseModule());

    long project;

    @Before
    public void setupService() {
        DataSource dataSource = injector.getInstance(DataSource.class);

        Sql.with(dataSource).exec("TRUNCATE TABLE ip_address", null);

        ProjectService projectService = new JdbcProjectService(dataSource);
        networkService = new JdbcNetworkService(dataSource);

        projectService.createProject("testNetwork", 1, 1);

        project = Sql.with(dataSource).exec("SELECT project_id FROM project",
                Sql.nextOrNull(this::mapUserAccount));
    }

    protected Long mapUserAccount(ResultSet rs) throws SQLException {
        return rs.getLong("project_id");
    }

    @Test
    public void testService() {

        long addressId = 123;

        networkService.createIpAddress(addressId, project);

        List<IpAddress> ips = networkService.listIpAddresses(project);
        assertEquals(1, ips.size());

        IpAddress ip = networkService.getIpAddress(addressId);

        assertEquals(project, ip.projectId);
        assertTrue(ip.validUntil.isAfter(Instant.now()));
        assertNotNull(ip.validOn);
        assertEquals(addressId, ip.ipAddressId);

        networkService.destroyIpAddress(addressId);

        IpAddress deletedIp = networkService.getIpAddress(addressId);
        assertTrue(deletedIp.validUntil.isBefore(ip.validUntil));
    }
}
