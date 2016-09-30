package com.godaddy.vps4.vm;

import org.apache.commons.validator.routines.InetAddressValidator;

import com.godaddy.vps4.Vps4Exception;

public class HostnameGenerator {

    public static String getHostname(String ipAddress) {

        InetAddressValidator.getInstance().isValid(ipAddress);
        validateIpAddress(ipAddress);
        return "s" + ipAddress.replace('.', '-') + ".secureserver.net";

    }

    private static void validateIpAddress(String ipAddress) {

        if (!InetAddressValidator.getInstance().isValid(ipAddress)) {
            throw new Vps4Exception("NOT_IP_ADDRESS", String.format("%s is not a valid IP address", ipAddress));

        }
    }
}
