package com.godaddy.vps4.oh.backups.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum OhBackupType {
    @JsonProperty("full") FULL,
    @JsonProperty("incremental") INCREMENTAL;

    @Override
    public String toString() {
        // the Java enum naming convention is uppercase, but the OH API only accepts lowercase
        return name().toLowerCase();
    }
}
