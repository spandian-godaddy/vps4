package com.godaddy.vps4;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.godaddy.vps4.security.Vps4User;

public class ExampleTest {

    @Test
    public void alwaysPasses() {

        Vps4User user = new Vps4User(0, "ShopperId");
        user.toString();

        assertTrue(true);
    }
}
