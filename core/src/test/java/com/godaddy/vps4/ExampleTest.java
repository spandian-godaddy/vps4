package com.godaddy.vps4;

import org.junit.Test;

import com.godaddy.vps4.security.User;

import static org.junit.Assert.*;

public class ExampleTest {

    @Test
    public void alwaysPasses() {

        User user = new User("Brian", 0, "ShopperId");

        assertTrue(true);
    }
}
