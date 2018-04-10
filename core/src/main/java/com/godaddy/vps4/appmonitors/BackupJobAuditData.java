package com.godaddy.vps4.appmonitors;

import java.time.Instant;
import java.util.UUID;

public class BackupJobAuditData {

    public UUID vmId;
    public Instant validOn;

    public BackupJobAuditData(UUID vmId, Instant validOn) {
        this.vmId = vmId;
        this.validOn = validOn;
    }

}
