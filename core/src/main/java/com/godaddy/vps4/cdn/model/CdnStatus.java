package com.godaddy.vps4.cdn.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum CdnStatus {
    @JsonProperty("PENDING") PENDING,
    @JsonProperty("FAILED") FAILED,
    @JsonProperty("SUCCESS") SUCCESS;
}
