package com.godaddy.vps4.oh.backups;

import com.godaddy.vps4.oh.backups.models.OhBackup;

public class NamedOhBackup extends OhBackup {
    public String name;

    public NamedOhBackup(OhBackup backup, String name) {
        this.id = backup.id;
        this.packageId = backup.packageId;
        this.jobId = backup.jobId;
        this.state = backup.state;
        this.purpose = backup.purpose;
        this.createdAt = backup.createdAt;
        this.modifiedAt = backup.modifiedAt;
        this.name = name;
    }
}
