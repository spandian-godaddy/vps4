package com.godaddy.vps4.phase2;

import org.junit.Test;
import static org.junit.Assert.*;

import com.godaddy.vps4.security.User;
import com.godaddy.vps4.web.UsersResource;

public class ExampleDatabaseTest {

    @Test
    public void alwaysPasses() {
        User user = new User();
        user.name = "Brian";

        new UsersResource(null, user).getUser();
        assertTrue(true);
    }
}
