package com.godaddy.vps4.oh.backups;

import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.oh.backups.models.OhBackup;
import com.godaddy.vps4.oh.backups.models.OhBackupState;
import com.godaddy.vps4.oh.jobs.models.OhJob;

public interface OhBackupService {
    /*
     * This function doubles as auth validation since it will return a 404 if the backup does not belong to the VM
     * provided. The other methods in this class don't need that validation, because it is assumed that getBackup will
     * be called first.
     */
    OhBackup getBackup(UUID vmId, UUID backupId);

    /*
     * Snapshots created through the HFS API will also show up in this list, since they are all the same as far as OH is
     * concerned. HFS snapshots (on-demand and automatic) and on-demand OH backups will both have the property
     * purpose=customer. On-demand OH backups are also stored in our database.
     */
    List<OhBackup> getBackups(UUID vmId, OhBackupState... state);

    OhBackup createBackup(UUID vmId);

    void deleteBackup(UUID vmId, UUID backupId);

    OhJob restoreBackup(UUID vmId, UUID backupId);
}
