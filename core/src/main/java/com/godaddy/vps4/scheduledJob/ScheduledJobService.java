package com.godaddy.vps4.scheduledJob;

import java.util.List;
import java.util.UUID;

public interface ScheduledJobService {
    
    List<ScheduledJob> getScheduledJobs(UUID vmId);
    
    List<ScheduledJob> getScheduledJobsByType(UUID vmId, ScheduledJob.ScheduledJobType type);
    
    ScheduledJob getScheduledJob(UUID id);
    
    void insertScheduledJob(UUID id, UUID vmId, ScheduledJob.ScheduledJobType type);

    void deleteScheduledJob(UUID id);
}
