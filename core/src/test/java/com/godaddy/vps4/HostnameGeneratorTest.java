package com.godaddy.vps4;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.godaddy.vps4.vm.HostnameGenerator;

public class HostnameGeneratorTest {

    @Test
    public void testGetHostname() {
        String ipAddress = "192.168.1.1";

        String hostname = HostnameGenerator.getHostname(ipAddress);

        assertEquals("ip-192-168-1-1.ip.secureserver.net", hostname);
    }
}
