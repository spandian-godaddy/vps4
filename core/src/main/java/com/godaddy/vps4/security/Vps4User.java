package com.godaddy.vps4.security;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.UUID;

public class Vps4User {

    private final long id;
    private final String shopperId;
    private final UUID customerId;

    public Vps4User(long id, String shopperId, UUID customerId) {
        this.id = id;
        this.shopperId = shopperId;
        this.customerId = customerId;
    }

    public long getId() {
        return id;
    }

    public String getShopperId() {
        return shopperId;
    }

    public UUID getCustomerId() { return customerId; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
