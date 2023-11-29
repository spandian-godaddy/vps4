package com.godaddy.vps4.entitlement.models;

public class EntitlementSuspendReinstateRequest {
    public String suspendReason;

    public EntitlementSuspendReinstateRequest(String suspendReason) {
        this.suspendReason = suspendReason;
    }
}
