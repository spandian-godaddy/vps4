package com.godaddy.vps4.network;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class IpAddress {

    public enum IpAddressType {
        PRIMARY(1), SECONDARY(2);

        private int id;

        private static Map<Integer, IpAddressType> map = stream(IpAddressType.values())
                .collect(toMap(ipType -> ipType.id, ipType -> ipType));

        IpAddressType(int id) {
            this.id = id;
        }

        public static IpAddressType valueOf(int id) {
            return map.get(id);
        }

        public int getId() {
            return id;
        }
    }

    public long id;
    public long ipAddressId;
    public UUID vmId;
    public String ipAddress;
    public IpAddressType ipAddressType;
    public Long pingCheckId;
    public Instant validOn;
    public Instant validUntil;

    public IpAddress() {
    }

    public IpAddress(long id, long ipAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType, Long pingCheckId, Instant validOn,
            Instant validUntil) {
        this.id = id;
        this.ipAddressId = ipAddressId;
        this.vmId = vmId;
        this.ipAddress = ipAddress;
        this.ipAddressType = ipAddressType;
        this.pingCheckId = pingCheckId;
        this.validOn = validOn;
        this.validUntil = validUntil;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
