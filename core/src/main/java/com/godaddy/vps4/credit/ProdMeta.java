package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.vm.DataCenter;

public class ProdMeta {

    public DataCenter dataCenter;
    public UUID productId;
    public Instant provisionDate;
    public boolean fullyManagedEmailSent;
    public Instant purchasedAt;
    public Instant releasedAt;
    public int relayCount;

    public ProdMeta() {}
}