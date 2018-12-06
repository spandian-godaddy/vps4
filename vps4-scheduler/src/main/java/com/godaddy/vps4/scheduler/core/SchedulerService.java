package com.godaddy.vps4.scheduler.core;

import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;

import java.util.List;
import java.util.UUID;

public interface SchedulerService {
    void startScheduler() throws Exception;
    void stopScheduler() throws Exception;
    void registerJobClassForJobGroup(String jobGroupId, Class<? extends SchedulerJob> jobClass);
    void registerTriggerListenerForJobGroup(String jobGroupId, SchedulerTriggerListener triggerListener) throws Exception;
    SchedulerJobDetail createJob(String product, String jobGroup, String requestJson) throws Exception;
    SchedulerJobDetail updateJobSchedule(String product, String jobGroup, UUID jobId, String requestJson) throws Exception;
    List<SchedulerJobDetail> getGroupJobs(String product, String jobGroup);
    SchedulerJobDetail getJob(String product, String jobGroup, UUID jobId) throws Exception;
    void deleteJob(String product, String jobGroup, UUID jobId) throws Exception;
    void pauseJob(String product, String jobGroup, UUID jobId) throws Exception;
    void resumeJob(String product, String jobGroup, UUID jobId) throws Exception;
}
