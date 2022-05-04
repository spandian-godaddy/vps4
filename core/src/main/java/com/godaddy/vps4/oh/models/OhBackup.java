package com.godaddy.vps4.oh.models;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/*
{
    "uuid": "20b50a1c-aedf-41ec-9604-fc201316b0e2",
    "package_uuid": "b48953bd-8042-40ed-b238-548d8c399e94",
    "job_uuid": "20b95a29-aedf-11ec-8727-c43772d79070",
    "state": "complete",
    "tag": "Created by vzseo-noded",
    "type": "incremental",
    "purpose": "dr",
    "cdate": 1648503389.162003,
    "mdate": 1648503393.44156
}
*/

public class OhBackup {
    private UUID id;
    private String state;
    private Instant createdAt;
    private Instant modifiedAt;

    @JsonSetter("uuid")
    public void setId(UUID id) {
        this.id = id;
    }

    @JsonGetter("id")
    public UUID getId() {
        return id;
    }

    @JsonSetter("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonGetter("state")
    public String getState() {
        return state;
    }

    @JsonSetter("cdate")
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @JsonGetter("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @JsonSetter("mdate")
    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    @JsonGetter("modifiedAt")
    public Instant getModifiedAt() {
        return modifiedAt;
    }
}
