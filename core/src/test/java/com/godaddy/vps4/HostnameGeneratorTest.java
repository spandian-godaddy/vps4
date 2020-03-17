package com.godaddy.vps4;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.godaddy.vps4.vm.HostnameGenerator;
import com.godaddy.vps4.vm.Image.OperatingSystem;

public class HostnameGeneratorTest {

    @Test
    public void testGetLinuxHostname() {
        String ipAddress = "192.168.1.1";
        OperatingSystem os = OperatingSystem.LINUX;

        String hostname = HostnameGenerator.getHostname(ipAddress, os);
        assertEquals("ip-192-168-1-1.ip.secureserver.net", hostname);
    }

    @Test
    public void testGetWindowsHostname() {
        String ipAddress = "192.168.1.1";
        OperatingSystem os = OperatingSystem.WINDOWS;

        String hostname = HostnameGenerator.getHostname(ipAddress, os);
        assertEquals("s192-168-1-1.secureserver.net", hostname);
    }
}
