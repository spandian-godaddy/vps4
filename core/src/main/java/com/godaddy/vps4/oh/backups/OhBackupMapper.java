package com.godaddy.vps4.oh.backups;

import java.util.UUID;

import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupPurpose;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.oh.jobs.models.OhJobStatus;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotAction;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

public class OhBackupMapper {
    public static Snapshot toSnapshot(OhBackup ohBackup, UUID vmId, long projectId) {
        String name = "Backup";
        SnapshotStatus status = toStatus(ohBackup.status);
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

    public static SnapshotAction toAction(OhBackup ohJob, ActionType type) {
        SnapshotAction action = new SnapshotAction();
        // TODO: how to map OH job ID (type: UUID) to SnapshotAction ID (type: long)?
        // action.snapshotId =
        action.created = ohJob.createdAt;
        action.type = type;
        action.status = toActionStatus(ohJob.status);
        action.completed = action.status == ActionStatus.COMPLETE ? ohJob.modifiedAt : null;
        return action;
    }

    private static ActionStatus toActionStatus(OhBackupState ohBackupState) {
        switch (ohBackupState) {
            case PENDING:
                return ActionStatus.NEW;
            case COMPLETE:
                return ActionStatus.COMPLETE;
        }
        return ActionStatus.ERROR;
    }

    private static ActionStatus toActionStatus(OhJobStatus ohJobStatus) {
        switch (ohJobStatus) {
            case PENDING:
                return ActionStatus.NEW;
            case STARTED:
            case RETRY:
                return ActionStatus.IN_PROGRESS;
            case SUCCESS:
                return ActionStatus.COMPLETE;
        }
        return ActionStatus.ERROR;
    }
}
