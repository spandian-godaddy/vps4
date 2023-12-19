package com.godaddy.vps4.firewall.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FirewallBypassWAF {
    @JsonProperty("enabled") ENABLED,
    @JsonProperty("disabled") DISABLED;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
