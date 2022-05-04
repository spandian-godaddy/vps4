package com.godaddy.vps4.oh;


import java.util.List;
import java.util.UUID;

import com.godaddy.vps4.oh.models.OhBackup;

public interface OhBackupService {
    List<OhBackup> getBackups(UUID vmId);
}
