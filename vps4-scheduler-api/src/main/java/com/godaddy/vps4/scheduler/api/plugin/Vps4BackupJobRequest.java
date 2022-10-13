package com.godaddy.vps4.scheduler.api.plugin;

import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduler.api.core.*;
import com.godaddy.vps4.vm.ServerType;
import com.godaddy.vps4.vm.ServerType.Platform;

import java.util.UUID;

@Product("vps4")
@JobGroup("backups")
public class Vps4BackupJobRequest extends JobRequest {
    @Required public UUID vmId;
    @Required public String backupName;
    @Required public String shopperId;
    @Required public ScheduledJob.ScheduledJobType scheduledJobType;
}
