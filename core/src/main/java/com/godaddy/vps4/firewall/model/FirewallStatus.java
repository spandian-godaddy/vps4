package com.godaddy.vps4.firewall.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FirewallStatus {
    @JsonProperty("PENDING") PENDING,
    @JsonProperty("FAILED") FAILED,
    @JsonProperty("SUCCESS") SUCCESS;
}
