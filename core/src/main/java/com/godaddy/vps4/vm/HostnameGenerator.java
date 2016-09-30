package com.godaddy.vps4.vm;

public class HostnameGenerator {

    public static String getHostname(String ipAddress) {

        return "s" + ipAddress.replace('.', '-') + ".secureserver.net";

    }
}
