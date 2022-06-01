package com.godaddy.vps4.oh.backups.models;

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

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public class OhBackup {
    @JsonAlias({"uuid"}) public UUID id;
    @JsonAlias({"job_uuid"}) public UUID jobId;
    @JsonAlias({"state"}) public OhBackupState status;
    @JsonAlias({"purpose"}) public OhBackupPurpose purpose;
    @JsonAlias({"cdate"}) public Instant createdAt;
    @JsonAlias({"mdate"}) public Instant modifiedAt;
}
