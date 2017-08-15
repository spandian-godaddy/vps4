package com.godaddy.vps4.snapshot;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonView;
import com.godaddy.vps4.security.Views;

public class Snapshot {
    public final UUID id;
    public final long projectId;
    public final UUID vmId;
    public final String name;
    public final SnapshotStatus status;
    public final Instant createdAt;
    public final Instant modifiedAt;
    public final SnapshotType snapshotType;

    @JsonView(Views.Internal.class)
    public final String hfsImageId;
    @JsonView(Views.Internal.class)
    public final long hfsSnapshotId;

    public Snapshot(UUID id,
                    long projectId,
                    UUID vmId,
                    String name,
                    SnapshotStatus status,
                    Instant createdAt,
                    Instant modifiedAt,
                    String hfsImageId,
                    long hfsSnapshotId,
                    SnapshotType snapshotType) {
        this.id = id;
        this.projectId = projectId;
        this.vmId = vmId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.hfsImageId = hfsImageId;
        this.hfsSnapshotId = hfsSnapshotId;
        this.snapshotType = snapshotType;
    }

    @Override
    public String toString() {
        return "Snapshot [id=" + id + ", projectId=" + projectId + ", vmId=" + vmId + ", name=" + name + ", status="
                + status + ", createdAt=" + createdAt + ", modifiedAt=" + modifiedAt + ", hfsImageId=" + hfsImageId
                + ", hfsSnapshotId=" + hfsSnapshotId + ", type=" + snapshotType + "]";
    }
}
