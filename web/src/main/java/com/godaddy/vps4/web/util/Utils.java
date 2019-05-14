package com.godaddy.vps4.web.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.StringUtils;

import com.godaddy.vps4.web.Vps4Exception;

public class Utils {
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    public static String getClientIpAddress(HttpHeaders headers, HttpServletRequest req) {
        String clientIpAddress = getIpAddressFromHeader(headers);

        if (clientIpAddress == null) {
            clientIpAddress = req.getRemoteAddr();
        }

        try {
            // make sure the address is an IPv4 address
            if (StringUtils.isNotBlank(clientIpAddress) && (isIPv4Address(clientIpAddress))) {
                return clientIpAddress;
            }
        } catch (Exception ex) {

        }

        throw new Vps4Exception("INVALID_CLIENT_IP", "Unable to determine client IP address.");
    }

    public static boolean isIPv4Address(String ipAddress) {
        try {
            return InetAddress.getByName(ipAddress) instanceof Inet4Address;
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    private static String getIpAddressFromHeader(HttpHeaders headers) {
        List<String> xForwardedFor = headers.getRequestHeader(X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.get(0).split(",")[ 0 ];
        }

        return null;
    }
}
