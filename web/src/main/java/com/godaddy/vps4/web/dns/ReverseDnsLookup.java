package com.godaddy.vps4.web.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.web.Vps4Exception;

public class ReverseDnsLookup {
    private static final Logger logger = LoggerFactory.getLogger(ReverseDnsLookup.class);

    ReverseDnsLookup() {
    }

    void validateReverseDnsName(String reverseDnsName, String ipAddress) throws Vps4Exception {
        if (StringUtils.isBlank(reverseDnsName)) {
            logger.error("Reverse DNS name not provided for reverse dns lookup.");
            throw new Vps4Exception("MISSING_REVERSE_DNS_NAME",
                                    "Missing reverse dns name. Reverse DNS name is required.");
        }
        if (StringUtils.isBlank(ipAddress)) {
            logger.error("Ip Address is not provided.");
            throw new Vps4Exception("MISSING_IP", "Ip Address is not provided.");
        }

        try {
            // check if A record exists (perform reverse lookup - domain name to IP lookup).
            InetAddress domainAddress = InetAddress.getByName(reverseDnsName);
            String _A_recordIpAddress = domainAddress.getHostAddress();
            if (!StringUtils.equalsIgnoreCase(ipAddress, _A_recordIpAddress)) {
                String errorMessage = String.format(
                        "Ip Address %s provided does not match the Ip address retrieved %s after reverse dns name " +
                                "lookup for  %s ",
                        ipAddress, _A_recordIpAddress, reverseDnsName);
                logger.warn(errorMessage);
                throw new Vps4Exception("IP_ADDRESS_LOOKUP_FAILED", errorMessage);
            }
            logger.info("Found IP address {} for Reverse Dns Name Lookup for domain {}", _A_recordIpAddress, reverseDnsName);
        } catch (UnknownHostException uhex) {
            logger.warn("Could not lookup ip address using reverse dns name {} ", reverseDnsName, uhex);
            throw new Vps4Exception("IP_ADDRESS_LOOKUP_FAILED",
                                    String.format("Could not lookup ip address using reverse dns name %s ",
                                                  reverseDnsName));
        }
    }
}
