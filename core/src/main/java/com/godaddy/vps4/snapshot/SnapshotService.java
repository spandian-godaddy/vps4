package com.godaddy.vps4.snapshot;

import java.util.List;
import java.util.UUID;

public interface SnapshotService {
    List<Snapshot> getSnapshotsForUser(long vps4UserId);

    List<Snapshot> getSnapshotsByOrionGuid(UUID orionGuid);

    Snapshot getSnapshot(UUID id);

    List<Snapshot> getSnapshotsForVm(UUID vmId);

    UUID createSnapshot(long projectId, UUID vmId, String name, SnapshotType snapshotType);

    boolean isOverQuota(UUID orionGuid, SnapshotType snapshotType);

    boolean otherBackupsInProgress(UUID orionGuid);

    void renameSnapshot(UUID snapshotId, String name);

    void updateHfsSnapshotId(UUID snapshotId, long hfsSnapshotId);

    void updateHfsImageId(UUID snapshotId, String hfsImageId);

    void markSnapshotInProgress(UUID snapshotId);

    void markSnapshotLive(UUID snapshotId);

    void markSnapshotErrored(UUID snapshotId);

    void markSnapshotDestroyed(UUID snapshotId);

    UUID markOldestSnapshotForDeprecation(UUID orionGuid, SnapshotType snapshotType);

    void markSnapshotAsDeprecated(UUID snapshotId);

    void reverseSnapshotDeprecation(UUID snapshotId);

    void updateSnapshotStatus(UUID snapshotId, SnapshotStatus status);

    void cancelErroredSnapshots(UUID orionGuid, SnapshotType snapshotType);

    int failedBackupsSinceSuccess(UUID vmId, SnapshotType snapshotType);
}
