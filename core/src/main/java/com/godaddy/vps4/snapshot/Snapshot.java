package com.godaddy.vps4.snapshot;

import java.time.Instant;
import java.util.UUID;

public class Snapshot {
    public final UUID id;
    public final long projectId;
    public final UUID vmId;
    public final String name;
    public final SnapshotStatus status;
    public final Instant createdAt;
    public final Instant modifiedAt;

    public Snapshot(UUID id,
                    long projectId,
                    UUID vmId,
                    String name,
                    SnapshotStatus status,
                    Instant createdAt,
                    Instant modifiedAt) {
        this.id = id;
        this.projectId = projectId;
        this.vmId = vmId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    @Override
    public String toString() {
        return "Snapshot [id=" + id
                + ", projectId=" + projectId
                + ", vmId=" + vmId
                + ", name=" + name
                + ", status=" + status
                + ", createdAt=" + createdAt
                + ", modifiedAt=" + modifiedAt + "]";
    }
}
