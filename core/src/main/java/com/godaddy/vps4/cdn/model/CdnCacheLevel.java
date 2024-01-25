package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CdnCacheLevel {
    CACHING_DISABLED,
    CACHING_OPTIMIZED;

    @Override
    public String toString() {
        return name().replace("_","");
    }
}
