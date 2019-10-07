package com.godaddy.vps4.vm;

public class HostnameGenerator {

    public static String getHostname(String ipAddress) {

        return "ip-" + ipAddress.replace('.', '-') + ".ip.secureserver.net";

    }
}
