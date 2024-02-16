package com.godaddy.vps4.entitlement.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

public class Entitlement {
    public String status;
    public UUID customerId;
    public UUID entitlementId;
    public String uri;
    public EntitlementMetadata metadata;
    public Product product;
    public String productKey;
    public String[] suspendReasons;
    public Current current;
    public String provisioningTracker;
    public Term term;
    public Entitlement() {
    }
}