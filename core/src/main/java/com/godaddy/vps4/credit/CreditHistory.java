package com.godaddy.vps4.credit;

import java.util.UUID;
import java.time.Instant;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class CreditHistory {
    public UUID vmId;
    public Instant validOn;
    public Instant validUntil;

    public CreditHistory(UUID vmId, Instant validOn, Instant validUntil) {
        this.vmId = vmId;
        this.validOn = validOn;
        this.validUntil = validUntil;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
