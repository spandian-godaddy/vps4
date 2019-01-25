package com.godaddy.vps4.orchestration.scheduler;

import java.util.HashMap;
import java.util.Map;

import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4RemoveSupportUserJobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;

public class Utils {
    private static final Map<ScheduledJob.ScheduledJobType, Class<? extends JobRequest>> typeClassMap = new HashMap<>();

    static {
        typeClassMap.put(ScheduledJob.ScheduledJobType.BACKUPS_RETRY, Vps4BackupJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.ZOMBIE, Vps4ZombieCleanupJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.REMOVE_SUPPORT_USER, Vps4RemoveSupportUserJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.BACKUPS_MANUAL, Vps4BackupJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.BACKUPS_AUTOMATIC, Vps4BackupJobRequest.class);
    }

    public static Class<? extends JobRequest> getJobRequestClassForType(ScheduledJob.ScheduledJobType scheduledJobType) {
        return typeClassMap.get(scheduledJobType);
    }
}
