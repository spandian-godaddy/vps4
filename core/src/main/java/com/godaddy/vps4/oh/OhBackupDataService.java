package com.godaddy.vps4.oh;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.oh.backups.OhBackupData;

public interface OhBackupDataService {
    void createBackup(UUID backupId, UUID vmId, String name);

    void destroyBackup(UUID backupId);

    OhBackupData getBackup(UUID ohBackupId);

    List<OhBackupData> getBackups(UUID vmId);

    int totalFilledSlots(UUID vmId);
}
