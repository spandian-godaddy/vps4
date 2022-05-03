package com.godaddy.vps4.vm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.godaddy.vps4.vm.Image.OperatingSystem;

public class HostnameGenerator {

    public static String getHostname(String ipAddress, OperatingSystem os) {
        if (os == OperatingSystem.WINDOWS)
            return getWindowsHostname(ipAddress);
        return getLinuxHostname(ipAddress);
    }

    public static String getLinuxHostname(String ipAddress) {
        // This format is important for cpanel SSO since it is set up to resolve in DNS
        return getReverseIp(ipAddress, ".") + ".host.secureserver.net";
    }

    public static String getWindowsHostname(String ipAddress) {
        // The special format is necessary for windows since ComputerName in Windows only includes up to the first period
        // Windows hostnames do not resolve in DNS
        return getReverseIp(ipAddress, "-") + ".host.secureserver.net";
    }

    private static String getReverseIp(String ipAddress, String delimiter) {
        List<String> ipSegments = Arrays.asList(ipAddress.split("\\."));
        Collections.reverse(ipSegments);
        return String.join(delimiter, ipSegments);
    }
}
