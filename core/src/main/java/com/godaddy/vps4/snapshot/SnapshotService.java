package com.godaddy.vps4.snapshot;

import java.util.List;
import java.util.UUID;

public interface SnapshotService {
    Snapshot getSnapshot(UUID id);

    List<Snapshot> getSnapshotsForVm(UUID vmId);

    UUID createSnapshot(long projectId, UUID vmId, String name, SnapshotType snapshotType);

    int totalFilledSlots(UUID orionGuid, SnapshotType snapshotType);

    boolean hasSnapshotInProgress(UUID orionGuid);

    int totalSnapshotsInProgress();

    void renameSnapshot(UUID snapshotId, String name);

    void updateHfsSnapshotId(UUID snapshotId, long hfsSnapshotId);

    void updateHfsImageId(UUID snapshotId, String hfsImageId);

    Snapshot getOldestLiveSnapshot(UUID orionGuid, SnapshotType type);

    UUID markOldestSnapshotForDeprecation(UUID orionGuid, SnapshotType snapshotType);

    void updateSnapshotStatus(UUID snapshotId, SnapshotStatus status);

    void cancelErroredSnapshots(UUID orionGuid, SnapshotType snapshotType);

    int failedBackupsSinceSuccess(UUID vmId, SnapshotType snapshotType);

    UUID getVmIdWithInProgressSnapshotOnHv(String hypervisorHostname);

    void saveVmHvForSnapshotTracking(UUID vmId, String hypervisorHostname);

    void deleteVmHvForSnapshotTracking(UUID vmId);
}
