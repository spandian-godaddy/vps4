package com.godaddy.vps4.web;

import static org.junit.Assert.assertEquals;

import com.godaddy.vps4.web.Version;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

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
