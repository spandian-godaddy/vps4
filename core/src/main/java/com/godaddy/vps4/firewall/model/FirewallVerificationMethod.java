package com.godaddy.vps4.firewall.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FirewallVerificationMethod {
    @JsonProperty("TXT") TXT,
    @JsonProperty("HTTP") HTTP;
}
