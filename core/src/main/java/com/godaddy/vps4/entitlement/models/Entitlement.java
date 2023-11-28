package com.godaddy.vps4.entitlement.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

public class Entitlement {
    public String status;
    public UUID customerId;
    public UUID entitlementId;
    public String provisioningTracker;
    public EntitlementMetadata metadata;
    public Entitlement() {
    }
}