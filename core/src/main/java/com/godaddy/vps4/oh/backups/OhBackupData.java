package com.godaddy.vps4.oh.backups;

import java.time.Instant;
import java.util.UUID;

public class OhBackupData {
    public UUID backupId;
    public UUID vmId;
    public Instant created;
    public Instant destroyed;
}
