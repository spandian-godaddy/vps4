package com.godaddy.vps4.security;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.UUID;

public class Vps4User {

    private long id;
    private String shopperId;
    private UUID customerId;
    private String resellerId;

    public Vps4User() {}

    public Vps4User(long id, String shopperId, UUID customerId, String resellerId) {
        this.id = id;
        this.shopperId = shopperId;
        this.customerId = customerId;
        this.resellerId = resellerId;
    }

    public long getId() {
        return id;
    }

    public String getShopperId() {
        return shopperId;
    }

    public UUID getCustomerId() { return customerId; }

    public String getResellerId() { return resellerId; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
