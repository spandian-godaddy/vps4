package com.godaddy.vps4.phase2.security;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.security.jdbc.JdbcVps4UserService;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class JdbcVps4UserServiceTest {

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

        Vps4User user1 = userService.getOrCreateUserForShopper(shopperId, "1", UUID.randomUUID());
        assertEquals(shopperId, user1.getShopperId());
        long user1Id = user1.getId();

        Vps4User user2 = userService.getOrCreateUserForShopper(shopperId, "1", UUID.randomUUID());
        assertEquals(user1.getId(), user2.getId());

        Vps4User user3 = userService.getUser(user1Id);
        assertEquals(shopperId, user3.getShopperId());
    }

    @Test(expected = RuntimeException.class)
    public void cannotCreateShopperWithNullCustomerId() {
        Vps4UserService userService = new JdbcVps4UserService(dataSource);
        userService.getOrCreateUserForShopper(shopperId, "1", null);
    }

    @Test
    public void getUserForShopperTestCustomerIdNotNull() {
        Vps4UserService userService = new JdbcVps4UserService(dataSource);

        UUID customerId = UUID.randomUUID();
        Vps4User user1 = userService.getOrCreateUserForShopper(shopperId, "1", customerId);

        assertEquals(shopperId, user1.getShopperId());
        assertEquals(customerId, user1.getCustomerId());
    }
}
