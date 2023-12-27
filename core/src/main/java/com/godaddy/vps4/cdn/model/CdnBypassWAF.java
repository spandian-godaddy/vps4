package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CdnBypassWAF {
    @JsonProperty("enabled") ENABLED,
    @JsonProperty("disabled") DISABLED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
