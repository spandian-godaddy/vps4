package com.godaddy.vps4.security;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class Vps4User {

    private final long id;
    private final String shopperId;

    public Vps4User(long id, String shopperId) {
        this.id = id;
        this.shopperId = shopperId;
    }

    public long getId() {
        return id;
    }

    public String getShopperId() {
        return shopperId;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
