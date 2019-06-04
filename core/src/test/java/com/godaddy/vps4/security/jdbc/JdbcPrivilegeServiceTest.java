package com.godaddy.vps4.security.jdbc;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.postgresql.PGStatement;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.project.UserProjectPrivilege;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcPrivilegeServiceTest {


    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    String resellerId = "1";
    String shopperId = "privilege-shopper";
    String projectName = "privilege-project";
    Timestamp infTimeStamp = new Timestamp(PGStatement.DATE_POSITIVE_INFINITY);
    Instant infinityInstant = infTimeStamp.toInstant();

    long userId;
    long projectId;

    @Before
    public void setup() {
        Sql.with(dataSource)
                .exec("INSERT INTO vps4_user (shopper_id, reseller_id) VALUES (?, ?)", null, shopperId, resellerId);
        Sql.with(dataSource).exec("INSERT INTO project (project_name) VALUES (?)", null, projectName);

        userId = Sql.with(dataSource)
                .exec("SELECT vps4_user_id FROM vps4_user WHERE shopper_id = ?",
                        Sql.nextOrNull(rs -> rs.getLong("vps4_user_id")), shopperId);
        projectId = Sql.with(dataSource)
                .exec("SELECT project_id FROM project WHERE project_name = ?",
                        Sql.nextOrNull(rs -> rs.getLong("project_id")), projectName);
        Sql.with(dataSource)
                .exec("INSERT INTO user_project_privilege (vps4_user_id, privilege_id, project_id, valid_on, " +
                                "valid_until) VALUES (?, ?, ?, now_utc(),now_utc())",
                        null, userId, 1, projectId);


    }

    @After
    public void teardown() {
        Sql.with(dataSource)
                .exec("DELETE FROM user_project_privilege WHERE vps4_user_id = ? AND project_id = ?", null, userId,
                        projectId);
        Sql.with(dataSource).exec("DELETE FROM vps4_user WHERE shopper_id =?", null, shopperId);
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_name =?", null, projectName);
    }

    @Test
    public void getActivePrivilege() {
        Sql.with(dataSource)
                .exec("INSERT INTO user_project_privilege (vps4_user_id, privilege_id, project_id, valid_on, " +
                                "valid_until) VALUES (?, ?, ?, now_utc(),'infinity')",
                        null, userId, 1, projectId);
        JdbcPrivilegeService jdbcPrivilegeService = new JdbcPrivilegeService(dataSource);
        UserProjectPrivilege userProjectPrivilege = jdbcPrivilegeService.getActivePrivilege(projectId);
        assertEquals(userProjectPrivilege.vps4UserId, userId);
        assertEquals(infinityInstant, userProjectPrivilege.validUntil);
    }

    @Test
    public void returnsNullForProjectIdThatDoesNotExist() {
        JdbcPrivilegeService jdbcPrivilegeService = new JdbcPrivilegeService(dataSource);
        UserProjectPrivilege userProjectPrivilege = jdbcPrivilegeService.getActivePrivilege(2000);
        assertNull(userProjectPrivilege);
    }


    @Test
    public void outdateVmPrivilegeForShopper() {
        Sql.with(dataSource)
                .exec("INSERT INTO user_project_privilege (vps4_user_id, privilege_id, project_id, valid_on, " +
                                "valid_until) VALUES (?, ?, ?, now_utc(),'infinity')",
                        null, userId, 1, projectId);
        JdbcPrivilegeService jdbcPrivilegeService = new JdbcPrivilegeService(dataSource);
        UserProjectPrivilege userProjectPrivilege = jdbcPrivilegeService.getActivePrivilege(projectId);

        assertEquals(userProjectPrivilege.vps4UserId, userId);
        assertEquals(infinityInstant, userProjectPrivilege.validUntil);


        jdbcPrivilegeService.outdateVmPrivilegeForShopper(userId, projectId);
        userProjectPrivilege = jdbcPrivilegeService.getActivePrivilege(projectId);
        assertEquals(userProjectPrivilege, null);

    }

    @Test
    public void addPrivilegeForUser() {
        JdbcPrivilegeService jdbcPrivilegeService = new JdbcPrivilegeService(dataSource);

        jdbcPrivilegeService.addPrivilegeForUser(userId, 1, projectId);

        UserProjectPrivilege userProjectPrivilege = jdbcPrivilegeService.getActivePrivilege(projectId);
        assertEquals(userProjectPrivilege.vps4UserId, userId);
        assertEquals(userProjectPrivilege.privilegeId, 1);
        assertEquals(infinityInstant, userProjectPrivilege.validUntil);

    }

}