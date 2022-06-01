package com.godaddy.vps4.oh.jobs.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OhJobStatus {
    @JsonProperty("pending") PENDING,
    @JsonProperty("started") STARTED,
    @JsonProperty("retry") RETRY,
    @JsonProperty("failure") FAILURE,
    @JsonProperty("success") SUCCESS,
    @JsonProperty("error") ERROR;

    @Override
    public String toString() {
        // the Java enum naming convention is uppercase, but the OH API only accepts lowercase
        return name().toLowerCase();
    }
}
