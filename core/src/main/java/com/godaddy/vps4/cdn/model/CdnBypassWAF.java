package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CdnBypassWAF {
    ENABLED,
    DISABLED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
