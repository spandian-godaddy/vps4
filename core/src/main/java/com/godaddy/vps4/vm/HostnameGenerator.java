package com.godaddy.vps4.vm;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.godaddy.vps4.Vps4Exception;

public class HostnameGenerator {

    public static String getHostname(String ipAddress) {

        validateIpAddress(ipAddress);
        return "s" + ipAddress.replace('.', '-') + ".secureserver.net";

    }

    private static void validateIpAddress(String ipAddress) {

        try {
            InetAddress.getByName(ipAddress);
        }
        catch (UnknownHostException e) {
            throw new Vps4Exception("INVALID_IP_ADDRESS", String.format("%s is not a valid IP address", ipAddress), e);
        }
    }
}
