package com.godaddy.vps4.prodMeta.model;

import java.util.UUID;

import com.godaddy.vps4.vm.DataCenter;

import java.time.Instant;

public class ProdMeta {
    public UUID entitlementId;
    public DataCenter dataCenter;
    public UUID productId;
    public Instant provisionDate;
    public boolean fullyManagedEmailSent;
    public Instant purchasedAt;
    public Instant releasedAt;
    public int relayCount;

    public ProdMeta() {}

    public ProdMeta(UUID entitlementId, DataCenter dataCenter, UUID productId, Instant provisionDate, boolean fullyManagedEmailSent, Instant purchasedAt, Instant releasedAt, int relayCount) {
        this.entitlementId = entitlementId;
        this.dataCenter = dataCenter;
        this.productId = productId;
        this.provisionDate = provisionDate;
        this.fullyManagedEmailSent = fullyManagedEmailSent;
        this.purchasedAt = purchasedAt;
        this.releasedAt = releasedAt;
        this.relayCount = relayCount;
    }
}
