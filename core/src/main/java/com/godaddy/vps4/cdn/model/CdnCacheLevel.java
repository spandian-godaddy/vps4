package com.godaddy.vps4.cdn.model;

public enum CdnCacheLevel {
    CACHING_DISABLED,
    CACHING_OPTIMIZED;

    @Override
    public String toString() {
        return name().replace("_","");
    }
}
