package com.godaddy.vps4;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.godaddy.vps4.vm.HostnameGenerator;

public class HostnameGeneratorTest {

    @Test
    public void testGetHostname() {
        String ipAddress = "192.168.1.1";

        String hostname = HostnameGenerator.getHostname(ipAddress);

        assertEquals("s192-168-1-1.secureserver.net", hostname);
    }

    @Test(expected = Vps4Exception.class)
    public void testGetHostnameInvalidCharacter() {
        String ipAddress = "a92.168.1.1";
        HostnameGenerator.getHostname(ipAddress);
    }

    @Test(expected = Vps4Exception.class)
    public void testGetHostnameInvalidIp() {
        String ipAddress = "192.168.1";
        HostnameGenerator.getHostname(ipAddress);
    }
}
