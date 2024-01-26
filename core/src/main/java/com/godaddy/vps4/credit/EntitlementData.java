package com.godaddy.vps4.credit;

import java.time.Instant;
import java.util.UUID;

import com.godaddy.vps4.vm.AccountStatus;

public class EntitlementData {
    public UUID entitlementId;
    public int tier;
    public int managedLevel;
    public String operatingSystem;
    public String controlPanel;
    public int monitoring;
    public AccountStatus accountStatus;
    public int pfid;
    public UUID customerId;
    public Instant expireDate;

    public EntitlementData() {}
}