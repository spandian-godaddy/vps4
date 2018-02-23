package com.godaddy.vps4.web;

import static org.junit.Assert.assertTrue;

import com.godaddy.vps4.config.Configs;
import com.google.inject.Injector;

import org.junit.Test;


public class Vps4ApplicationTest {
    @Test
    public void testNewInjector() {
        Configs.getInstance();

        Vps4Application app = new Vps4Application();
        Injector injector = app.newInjector();
        assertTrue(injector instanceof Injector);
    }
}
