package com.godaddy.vps4.cdn.model;

import com.godaddy.vps4.network.IpAddress;

import java.time.Instant;
import java.util.UUID;

public class VmCdnSite {
    public UUID vmId;
    public IpAddress ipAddress;
    public String domain;
    public String siteId;
    public Instant validOn;
    public Instant validUntil;
    public VmCdnSite(UUID vmId, IpAddress ipAddress, String domain, String siteId, Instant validOn, Instant validUntil) {
        this.vmId = vmId;
        this.ipAddress = ipAddress;
        this.domain = domain;
        this.siteId = siteId;
        this.validOn = validOn;
        this.validUntil = validUntil;
    }

    public VmCdnSite() {
    }
}
