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
    public final Long pingCheckId;
    public final Instant validOn;
    public final Instant validUntil;

    public IpAddress(long ipAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType, Long pingCheckId, Instant validOn,
            Instant validUntil) {
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
        StringBuilder result = new StringBuilder();
        result.append("IpAddress [ipAddressId=");
        result.append(ipAddressId);
        result.append(" vmId=");
        result.append(vmId);
        result.append(" validOn=");
        result.append(validOn);
        result.append(" validUntil");
        result.append(validUntil);
        result.append("]");
        return result.toString();
    }
}
