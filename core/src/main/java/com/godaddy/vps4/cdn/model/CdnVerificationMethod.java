package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CdnVerificationMethod {
    @JsonProperty("TXT") TXT,
    @JsonProperty("HTTP") HTTP;
}
