package com.godaddy.vps4.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpAddressValidator {

    /**
     * Validates an IP address to be in a valid format. Throws InvalidIpAddressException if the IP is not valid.
     * 
     * @param ipAddress
     */
    public static void validateIpAddress(String ipAddress) {

        try {
            InetAddress.getByName(ipAddress);
        }
        catch (UnknownHostException e) {
            throw new RuntimeException(String.format("%s is not a valid IP address", ipAddress), e);
        }
    }
}