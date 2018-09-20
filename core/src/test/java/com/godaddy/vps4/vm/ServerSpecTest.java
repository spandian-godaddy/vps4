package com.godaddy.vps4.vm;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;

public class ServerSpecTest {

    @Before
    public void setUp() throws Exception {}


    @Test
    public void testSpecIsVirtual() throws Exception {
        ServerType serverType = new ServerType();
        serverType.serverType = ServerType.Type.VIRTUAL;
        ServerSpec spec = new ServerSpec();
        spec.serverType = serverType;
        assertTrue(spec.isVirtualMachine());
    }

    @Test
    public void testSpecIsDedicated() throws Exception {
        ServerType serverType = new ServerType();
        serverType.serverType = ServerType.Type.DEDICATED;
        ServerSpec spec = new ServerSpec();
        spec.serverType = serverType;
        assertFalse(spec.isVirtualMachine());
    }
}
