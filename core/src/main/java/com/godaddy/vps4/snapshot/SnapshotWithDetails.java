package com.godaddy.vps4.snapshot;

import java.time.Instant;
import java.util.UUID;

public class SnapshotWithDetails extends Snapshot {
    public final String hfsImageId;
    public final long hfsSnapshotId;

    public SnapshotWithDetails(UUID id,
                               String hfsImageId,
                               long projectId,
                               long hfsSnapshotId,
                               UUID vmId,
                               String name,
                               SnapshotStatus status,
                               Instant createdAt,
                               Instant modifiedAt) {
        super(id, projectId, vmId, name, status, createdAt, modifiedAt);
        this.hfsImageId = hfsImageId;
        this.hfsSnapshotId = hfsSnapshotId;
    }

    @Override
    public String toString() {
        return "Snapshot [id=" + id
                + ", hfsImageId=" + hfsImageId
                + ", projectId=" + projectId
                + ", hfsSnapshotId=" + hfsSnapshotId
                + ", vmId=" + vmId
                + ", name=" + name
                + ", status=" + status + "]";
    }
}
