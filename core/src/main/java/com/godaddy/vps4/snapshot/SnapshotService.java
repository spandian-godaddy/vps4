package com.godaddy.vps4.snapshot;

import java.util.List;
import java.util.UUID;

public interface SnapshotService {
    List<Snapshot> getSnapshotsForUser(long vps4UserId);

    Snapshot getSnapshot(UUID id);

    SnapshotWithDetails getSnapshotWithDetails(UUID id);

    List<Snapshot> getSnapshotsForVm(UUID vmId);
}
