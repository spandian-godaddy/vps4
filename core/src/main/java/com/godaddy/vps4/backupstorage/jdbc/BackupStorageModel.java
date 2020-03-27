package com.godaddy.vps4.backupstorage.jdbc;

import java.time.Instant;
import java.util.UUID;

public class BackupStorageModel {
    public long id;
    public UUID vmId;
    public String ftpServer;
    public String ftpUser;
    public Instant validOn;
    public Instant validUntil;

    public BackupStorageModel() {}

    public BackupStorageModel(long id, UUID vmId,
                              String ftpServer, String ftpUser,
                              Instant validOn, Instant validUntil) {
        this.id = id;
        this.vmId = vmId;
        this.ftpServer = ftpServer;
        this.ftpUser = ftpUser;
        this.validOn = validOn;
        this.validUntil = validUntil;
    }
}
