package com.godaddy.vps4.cdn.model;

public enum CdnBypassWAF {
    ENABLED,
    DISABLED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
