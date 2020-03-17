package com.godaddy.vps4.vm;

import com.godaddy.vps4.vm.Image.OperatingSystem;

public class HostnameGenerator {

    public static String getHostname(String ipAddress, OperatingSystem os) {
        if (os == OperatingSystem.WINDOWS)
            return getWindowsHostname(ipAddress);
        return getLinuxHostname(ipAddress);
    }

    public static String getLinuxHostname(String ipAddress) {
        // The ip-* format is important for cpanel SSO since it is setup to resolve in DNS
        return "ip-" + ipAddress.replace('.', '-') + ".ip.secureserver.net";
    }

    public static String getWindowsHostname(String ipAddress) {
        // The s* format is necessary for windows since server name in windows is limited to 16 chars
        return "s" + ipAddress.replace('.', '-') + ".secureserver.net";
    }
}
