package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CdnCacheLevel {
    @JsonProperty("CACHINGDISABLED") CACHING_DISABLED,
    @JsonProperty("CACHINGOPTIMIZED") CACHING_OPTIMIZED;

    @Override
    public String toString() {
        return name().replace("_","");
    }
}
