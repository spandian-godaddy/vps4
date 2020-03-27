package com.godaddy.vps4.backupstorage;

import java.util.UUID;

import com.godaddy.vps4.backupstorage.jdbc.BackupStorageModel;

public interface BackupStorageService {
    void createBackupStorage(UUID vmId);

    void destroyBackupStorage(UUID vmId);

    BackupStorageModel getBackupStorage(UUID vmId);

    void setBackupStorage(UUID vmId, String ftpServer, String ftpUser);
}
