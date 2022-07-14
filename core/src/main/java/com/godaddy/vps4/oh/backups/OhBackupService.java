package com.godaddy.vps4.oh.backups;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.oh.jobs.models.OhJob;

public interface OhBackupService {
    OhBackup getBackup(UUID vmId, UUID backupId);

    List<OhBackup> getBackups(UUID vmId, OhBackupState... state);

    OhBackup createBackup(UUID vmId);

    void deleteBackup(UUID vmId, UUID backupId);

    OhJob restoreBackup(UUID vmId, UUID backupId);
}
