package com.godaddy.vps4.snapshot;

import java.util.List;
import java.util.UUID;

public interface SnapshotService {
    List<Snapshot> getSnapshotsForUser(long vps4UserId);

    List<SnapshotWithDetails> getSnapshotsByOrionGuid(UUID orionGuid);

    Snapshot getSnapshot(UUID id);

    SnapshotWithDetails getSnapshotWithDetails(UUID id);

    List<Snapshot> getSnapshotsForVm(UUID vmId);

    UUID createSnapshot(long projectId, UUID vmId, String name);

    boolean isOverQuota(UUID vmId);

    void updateHfsSnapshotId(UUID snapshotId, long hfsSnapshotId);

    void updateHfsImageId(UUID snapshotId, String hfsImageId);

    void markSnapshotInProgress(UUID snapshotId);

    void markSnapshotComplete(UUID snapshotId);

    void markSnapshotErrored(UUID snapshotId);

    void markSnapshotDestroyed(UUID snapshotId);
}
