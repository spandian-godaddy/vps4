package com.godaddy.vps4.web;

import static org.junit.Assert.assertTrue;

import com.google.inject.Injector;
import org.junit.Test;


public class Vps4ApplicationTest {
    @Test
    public void testNewInjector() {
        Vps4Application app = new Vps4Application();
        Injector injector = app.newInjector();
        assertTrue(injector instanceof Injector);
    }
}
