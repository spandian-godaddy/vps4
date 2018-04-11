package com.godaddy.vps4.phase2.security;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class JdbcUserServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);

    String shopperId = "testShopperId";

    @Before
    public void start() {
        Sql.with(dataSource).exec("DELETE FROM vps4_user WHERE shopper_id = ?", null, shopperId);
    }

    @After
    public void cleanup() {
        Sql.with(dataSource).exec("DELETE FROM vps4_user WHERE shopper_id = ?", null, shopperId);
    }

    @Test
    public void getOrCreateUserForShopperTest() {
        Vps4UserService userService = new JdbcVps4UserService(dataSource);

        Vps4User user1 = userService.getOrCreateUserForShopper(shopperId, "1");
        assertEquals(shopperId, user1.getShopperId());
        long user1Id = user1.getId();

        Vps4User user2 = userService.getOrCreateUserForShopper(shopperId, "1");
        assertEquals(user1.getId(), user2.getId());

        Vps4User user3 = userService.getUser(user1Id);
        assertEquals(shopperId, user3.getShopperId());
    }

}
