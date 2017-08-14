package com.godaddy.vps4.web;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VersionTest {
    @Test
    public void testDevGetCurrentVersion() {
        Package pkg = Version.class.getPackage();
        String expectedVersion = pkg.getImplementationVersion();
        if (expectedVersion == null) {
            expectedVersion = "dev";
        }

        assertEquals(expectedVersion, Version.CURRENT);
    }
}
