package com.godaddy.vps4.oh.backups;

import java.util.UUID;

import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupPurpose;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;

public class OhBackupMapper {
    public static Snapshot toSnapshot(OhBackup ohBackup, UUID vmId, long projectId) {
        String name = "Backup";
        SnapshotStatus status = toStatus(ohBackup.state);
        SnapshotType type = toType(ohBackup.purpose);

        return new Snapshot(ohBackup.id, projectId, vmId, name, status, ohBackup.createdAt,
                            ohBackup.modifiedAt, null, null, type);
    }

    private static SnapshotStatus toStatus(OhBackupState ohBackupState) {
        switch (ohBackupState) {
            case PENDING:
                return SnapshotStatus.IN_PROGRESS;
            case COMPLETE:
                return SnapshotStatus.LIVE;
            case DELETED:
                return SnapshotStatus.DESTROYED;
        }
        return SnapshotStatus.ERROR;
    }

    private static SnapshotType toType(OhBackupPurpose ohBackupPurpose) {
        return (ohBackupPurpose == OhBackupPurpose.CUSTOMER)
                ? SnapshotType.ON_DEMAND
                : SnapshotType.AUTOMATIC;
    }
}
