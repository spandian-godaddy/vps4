package com.godaddy.vps4.security.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.postgresql.PGStatement;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;

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
        Sql.with(dataSource).exec("INSERT INTO vps4_user (shopper_id, reseller_id) VALUES (?, ?)", null, shopperId, resellerId);
        userId = Sql.with(dataSource).exec("SELECT vps4_user_id FROM vps4_user WHERE shopper_id = ?",
                        Sql.nextOrNull(rs -> rs.getLong("vps4_user_id")), shopperId);

        Sql.with(dataSource).exec("INSERT INTO project (project_name, vps4_user_id) VALUES (?, ?)", null, projectName, userId);
        projectId = Sql.with(dataSource).exec("SELECT project_id FROM project WHERE project_name = ?",
                        Sql.nextOrNull(rs -> rs.getLong("project_id")), projectName);
    }

    @After
    public void teardown() {
        Sql.with(dataSource).exec("DELETE FROM project WHERE project_name =?", null, projectName);
        Sql.with(dataSource).exec("DELETE FROM vps4_user WHERE shopper_id =?", null, shopperId);
        }

}