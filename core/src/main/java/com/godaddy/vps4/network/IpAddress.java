package com.godaddy.vps4.network;

import java.time.Instant;

public class IpAddress {

    public final long ipAddressId;
    public final long projectId;
    public final Instant validOn;
    public final Instant validUntil;

    public IpAddress(long ipAddressId, long projectId, Instant validOn, Instant validUntil) {
        this.ipAddressId = ipAddressId;
        this.projectId = projectId;
        this.validOn = validOn;
        this.validUntil = validUntil;
    }

    @Override
    public String toString() {
        return "IpAddress [ipAddressId=" + ipAddressId + " projectId=" + projectId + " validOn=" + validOn + "validUntil" + validUntil
                + "]";
    }
}
