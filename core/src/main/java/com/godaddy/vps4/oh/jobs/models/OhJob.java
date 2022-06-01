package com.godaddy.vps4.oh.jobs.models;

/*
{
    "uuid": "dbd7732a-505d-43a6-9cb6-6b09f3a10ade",
    "type": "backup",
    "package_uuid": "e220299a-56c3-4469-bc16-2005fea98384",
    "node_status": "started",
    "node_jobuuid": "27055d1f-701c-496b-8ce9-3879e086d8ae",
    "lastpoll": 1653585326.405462,
    "cdate": 1653585276.437251,
    "mdate": 1653585326.405604
}
 */

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;

public class OhJob {
    @JsonAlias({"uuid"}) public UUID id;
    @JsonAlias({"node_status"}) public OhJobStatus status;
    @JsonAlias({"cdate"}) public Instant createdAt;
    @JsonAlias({"mdate"}) public Instant modifiedAt;
}
