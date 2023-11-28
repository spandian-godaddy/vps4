package com.godaddy.vps4.entitlement.models;

public class EntitlementCancelRequest {
    public String cancellationReason;
    public EntitlementCancelRequest(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
