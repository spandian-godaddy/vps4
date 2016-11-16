package com.godaddy.vps4;

import org.junit.Test;

import com.godaddy.vps4.security.Vps4User;

import static org.junit.Assert.*;

public class ExampleTest {

    @Test
    public void alwaysPasses() {

        Vps4User user = new Vps4User("Brian", 0, "ShopperId");

        assertTrue(true);
    }
}
