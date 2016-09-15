package com.godaddy.vps4.phase2.project.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.project.Project;
import com.godaddy.vps4.project.ProjectService;
import com.godaddy.vps4.project.jdbc.JdbcProjectService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcProjectServiceTest {

	Injector injector = Guice.createInjector(new DatabaseModule());
    private DataSource dataSource;
    private UUID accountUUID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Before
    @After
    public void truncate() throws SQLException {
        if (dataSource == null) {
            dataSource = injector.getInstance(DataSource.class);
        }
        try (Connection conn = dataSource.getConnection()) {
            try (Statement statement = conn.createStatement()) {
                statement.executeUpdate("TRUNCATE TABLE user_service_group_privilege CASCADE;");
                statement.executeUpdate("TRUNCATE TABLE service_group CASCADE;");
                statement.executeUpdate("TRUNCATE TABLE user_account CASCADE;");
                statement.executeUpdate("TRUNCATE TABLE mcs_user CASCADE;");
            }
            try (Statement statement = conn.createStatement()) {
                statement.executeUpdate("INSERT INTO mcs_user(user_id, shopper_id) VALUES (1, 'testuser1');");
                statement.executeUpdate("INSERT INTO mcs_user(user_id, shopper_id) VALUES (2, 'testuser2');");
                statement.executeUpdate("INSERT INTO user_account(user_id, account_uuid) VALUES (1,'"
                        + java.util.UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11") + "' )");
                statement.executeUpdate("INSERT INTO user_account(user_id, account_uuid) VALUES (2,'"
                        + java.util.UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12") + "' )");
                Sql.with(dataSource).exec("SELECT create_service_group('testServiceGroup1', ?, ?, ?)",
                        Sql.nextOrNull(rs -> rs.getLong(1)), 1,
                        java.util.UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"), (short) 1);
                Sql.with(dataSource).exec("SELECT create_service_group('testServiceGroup2', ?, ?, ?)",
                        Sql.nextOrNull(rs -> rs.getLong(1)), 1,
                        java.util.UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"), (short) 1);
                Sql.with(dataSource).exec("SELECT create_service_group('testServiceGroup3', ?, ?, ?)",
                        Sql.nextOrNull(rs -> rs.getLong(1)), 2,
                        java.util.UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"), (short) 1);
                Sql.with(dataSource).exec("SELECT create_service_group('testServiceGroup4', ?, ?, ?)",
                        Sql.nextOrNull(rs -> rs.getLong(1)), 2,
                        java.util.UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"), (short) 1);
                statement.executeUpdate("INSERT INTO user_account (user_id, account_uuid) VALUES (1, CAST('"
                        + accountUUID + "' AS UUID))");
            }
        }
    }

    @Test
    public void testGetServiceGroups() {
		ProjectService sgs = new JdbcProjectService(dataSource);
        List<Project> serviceGroups = sgs.getProjects(1, true);
        assertEquals(2, serviceGroups.size());
        Project group1 = serviceGroups.stream().filter(group -> group.getName().equals("testServiceGroup1")).findFirst()
                .get();
        Project group2 = serviceGroups.stream().filter(group -> group.getName().equals("testServiceGroup2")).findFirst()
                .get();
        assertNotNull(group1);
        assertNotNull(group2);
        serviceGroups = sgs.getProjects(3, true);
        assertEquals(0, serviceGroups.size());
    }

    @Test
    public void testCreateServiceGroup() {
        ProjectService sgs = new JdbcProjectService(dataSource);

        String serviceGroupName = "testServiceGroup";
        UUID account = accountUUID;

        Project serviceGroup = sgs.createProject(serviceGroupName, 1);
        assertTrue(serviceGroup.getSgid() > 0);
        assertEquals(serviceGroupName, serviceGroup.getName());
        assertEquals("mcs-" + serviceGroup.getSgid(), serviceGroup.getVhfsSgid());
//        assertEquals(account, serviceGroup.getBillingAccountUuid());
    }
}
