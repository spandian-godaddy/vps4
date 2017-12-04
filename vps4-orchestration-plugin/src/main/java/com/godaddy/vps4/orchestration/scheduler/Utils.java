package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;

import java.util.HashMap;
import java.util.Map;

public class Utils {
    private static final Map<ScheduledJob.ScheduledJobType, Class<? extends JobRequest>> typeClassMap = new HashMap();

    static {
        typeClassMap.put(ScheduledJob.ScheduledJobType.BACKUPS, Vps4BackupJobRequest.class);
        typeClassMap.put(ScheduledJob.ScheduledJobType.ZOMBIE, Vps4ZombieCleanupJobRequest.class);
    }

    public static Class<? extends JobRequest> getJobRequestClassForType(ScheduledJob.ScheduledJobType scheduledJobType) {
        return typeClassMap.get(scheduledJobType);
    }
}
