package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.Vps4User;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ExampleDatabaseTest {

    Injector injector = Guice.createInjector(new DatabaseModule());

    @Test
    public void testTheDatabase() throws SQLException {

        DataSource dataSource = injector.getInstance(DataSource.class);

        try (Connection conn = dataSource.getConnection()) {

            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM information_schema.tables")) {

                    while (rs.next()) {
                        System.out.println(rs.getString("table_schema") + "." + rs.getString("table_name"));
                    }
                }
            }

        }

        @SuppressWarnings("unused")
        Vps4User user = new Vps4User(0, "ShopperId", "1");

        assertTrue(true);
    }
}
