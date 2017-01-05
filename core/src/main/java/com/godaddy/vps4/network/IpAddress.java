package com.godaddy.vps4.network;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class IpAddress {

    public enum IpAddressType {
        PRIMARY(1), SECONDARY(2);

        private final int id;

        private final static Map<Integer, IpAddressType> map = stream(IpAddressType.values())
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

    public final long ipAddressId;
    public final UUID vmId;
    public final String ipAddress;
    public final IpAddressType ipAddressType;
    public final Instant validOn;
    public final Instant validUntil;

    public IpAddress(long ipAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType, Instant validOn, Instant validUntil) {
        this.ipAddressId = ipAddressId;
        this.vmId = vmId;
        this.ipAddress = ipAddress;
        this.ipAddressType = ipAddressType;
        this.validOn = validOn;
        this.validUntil = validUntil;
    }

    @Override
    public String toString() {
        return "IpAddress [ipAddressId=" + ipAddressId + " vmId=" + vmId + " validOn=" + validOn + "validUntil" + validUntil
                + "]";
    }
}
