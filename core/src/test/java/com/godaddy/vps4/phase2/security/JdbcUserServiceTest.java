package com.godaddy.vps4.phase2.security;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.security.Vps4User;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.security.Vps4UserService;
import gdg.hfs.security.jdbc.JdbcVps4UserService;

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

        Vps4User user1 = userService.getOrCreateUserForShopper(shopperId);
        assertEquals(shopperId, user1.getShopperId());
        long user1Id = user1.getId();

        Vps4User user2 = userService.getOrCreateUserForShopper(shopperId);
        assertEquals(user1.getId(), user2.getId());

        Vps4User user3 = userService.getUserForId(user1Id);
        assertEquals(shopperId, user3.getShopperId());
    }

}
