package com.godaddy.vps4.scheduler.core.quartz;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.quartz.impl.matchers.GroupMatcher.triggerGroupEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.utils.Utils;
import com.google.inject.Inject;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuartzSchedulerService implements SchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(QuartzSchedulerService.class);

    private final Scheduler scheduler;
    private final ObjectMapper objectMapper;
    private static final Map<String, Class<? extends SchedulerJob>> jobGroupToClassMapping;

    static {
       jobGroupToClassMapping = new HashMap<>();
    }

    @Inject
    public QuartzSchedulerService(Scheduler scheduler, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
    }

    private Class<? extends JobRequest> getJobRequestClass(Class<? extends SchedulerJob> jobClass) throws Exception {
        return Utils.getRequestClassForJobClass(jobClass);
    }

    private JobDetail buildJob(String groupId, Class<? extends SchedulerJob> jobClass, String jobDataJson)
            throws Exception {
        String jobId = UUID.randomUUID().toString();
        JobBuilder jobBuilder = newJob(jobClass).withIdentity(jobId, groupId);
        jobBuilder.usingJobData("jobDataJson", jobDataJson);
        jobBuilder.usingJobData("jobRequestClass", getJobRequestClass(jobClass).getName());
        return jobBuilder.build();
    }

    private Trigger buildTrigger(String groupName, JobRequest jobRequest) {
        String triggerName = UUID.randomUUID().toString();
        SimpleScheduleBuilder schedule = buildTriggerSchedule(jobRequest);
        return newTrigger()
           .withIdentity(triggerName, groupName)
           .startAt(Date.from(jobRequest.when))
           .withSchedule(schedule)
           .build();
    }

    private SimpleScheduleBuilder buildTriggerSchedule(JobRequest jobRequest) {
        SimpleScheduleBuilder schedule = simpleSchedule();

        if (jobRequest.jobType.equals(JobType.ONE_TIME)) {
            schedule = schedule.withRepeatCount(0)
                        .withMisfireHandlingInstructionFireNow();
            // upon a mis-fire situation, the trigger wants to be fired now by Scheduler
        }
        else {
            schedule = jobRequest.repeatCount != null
                ? schedule.withRepeatCount(jobRequest.repeatCount)
                : schedule.repeatForever();
            schedule = schedule.withIntervalInHours(jobRequest.repeatIntervalInDays * 24)
                                .withMisfireHandlingInstructionNextWithRemainingCount();
            // upon a mis-fire situation, fire the trigger at the next scheduled time
        }

        return schedule;
    }

    private SchedulerJobDetail scheduleJob(String product, String jobGroup,
                                           Class<? extends SchedulerJob> jobClass,
                                           String jobDataJson,
                                           JobRequest jobRequest)
        throws Exception
    {
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        JobDetail jobDetail = buildJob(jobGroupId, jobClass, jobDataJson);
        Trigger trigger = buildTrigger(jobGroupId, jobRequest);
        scheduler.scheduleJob(jobDetail, trigger);

        return getSchedulerJobDetail(jobDetail.getKey());
    }

    private SchedulerJobDetail getSchedulerJobDetail(JobKey jobKey) throws Exception {
        Trigger trigger = getExistingTriggerForJob(jobKey);
        Instant nextRun = null;
        JobDetail jobDetail = null;
        jobDetail = scheduler.getJobDetail(jobKey);

        try {
            jobDetail = scheduler.getJobDetail(jobKey);
        }
        catch (Exception e) {
            logger.error("Error while getting job detail for trigger {} for job detail: {}. Error details: {}",
                    trigger.toString(), jobDetail.toString(), e);
            throw new RuntimeException(e);
        }

        try {
            nextRun = trigger.getNextFireTime().toInstant();
        }
        catch (Exception e) {
            logger.error("Error while getting fire time for trigger {} for job detail: {}. Error details: {}",
                    trigger.toString(), jobDetail.toString(), e);
            throw new RuntimeException(e);
        }

        boolean isTriggerPaused = isTriggerPaused(trigger);
        // Get the job request data
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        String jobDataJson = jobDataMap.getString("jobDataJson");
        Class<? extends SchedulerJob> jobClass = getJobClassForGroup(jobKey.getGroup());
        JobRequest jobRequest = objectMapper.readValue(jobDataJson, getJobRequestClass(jobClass));

        return new SchedulerJobDetail(UUID.fromString(jobKey.getName()), nextRun, jobRequest, isTriggerPaused);
    }

    private boolean isTriggerPaused(Trigger trigger) throws Exception{
        return scheduler.getTriggerState(trigger.getKey()).equals(Trigger.TriggerState.PAUSED);
    }

    private boolean isJobPresent(String product, String jobGroup, UUID jobId) throws SchedulerException {
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        return scheduler.checkExists(new JobKey(jobId.toString(), jobGroupId));
    }

    private boolean hasJobClassBeenRegisteredForJobGroup(String jobGroupId) {
        return jobGroupToClassMapping.containsKey(jobGroupId);
    }

    private Class<? extends SchedulerJob> getJobClassForGroup(String jobGroupId) {
        return jobGroupToClassMapping.get(jobGroupId);
    }

    private Trigger getExistingTriggerForJob(JobKey jobKey) throws SchedulerException {
        return scheduler.getTriggersOfJob(jobKey).get(0);
    }

    private void replaceJobTrigger(JobRequest jobRequest, String jobGroupId, JobKey jobKey) throws SchedulerException {
        Trigger oldTrigger = getExistingTriggerForJob(jobKey);
        Trigger newTrigger = buildTrigger(jobGroupId, jobRequest);
        scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger);
    }

    @Override
    public SchedulerJobDetail createJob(String product, String jobGroup, String requestJson) throws Exception {
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        if (hasJobClassBeenRegisteredForJobGroup(jobGroupId)) {
            Class<? extends SchedulerJob> jobClass = getJobClassForGroup(jobGroupId);
            JobRequest jobRequest = objectMapper.readValue(requestJson, getJobRequestClass(jobClass));
            if (jobRequest.isValid()) {
                return scheduleJob(product, jobGroup, jobClass, requestJson, jobRequest);
            }
            else {
                logger.info("Job creation validation checks failed: {}", jobRequest.getExceptions());
                throw new Exception("Job creation validation checks failed");
            }
        }

        throw new Exception("Couldn't create job");
    }

    @Override
    public List<SchedulerJobDetail> getGroupJobs(String product, String jobGroup) {
        List<SchedulerJobDetail> jobs = new ArrayList<>();
        try {
            for (JobKey jobKey: scheduler.getJobKeys(jobGroupEquals(Utils.getJobGroupId(product, jobGroup)))) {
                try {
                    SchedulerJobDetail schedulerJobDetail = getSchedulerJobDetail(jobKey);
                    jobs.add(schedulerJobDetail);
                }
                catch (Exception e) {
                    logger.error("Error while listing jobs for group {}: {}", jobKey.getName(), e);
                }
            }

            return jobs;
        }
        catch (SchedulerException e) {
            logger.error("Scheduler exception while listing group jobs");
        }

        return new ArrayList<>();
    }

    @Override
    public SchedulerJobDetail updateJobSchedule(String product, String jobGroup, UUID jobId, String requestJson)
        throws Exception {
        if (isJobPresent(product, jobGroup, jobId)) {
            try {
                JobRequest jobRequest = objectMapper.readValue(requestJson, JobRequest.class);
                if (jobRequest.isValid()) {
                    String jobGroupId = Utils.getJobGroupId(product, jobGroup);
                    JobDetail jobDetail = scheduler.getJobDetail(
                            jobKey(jobId.toString(), Utils.getJobGroupId(product, jobGroup)));
                    replaceJobTrigger(jobRequest, jobGroupId, jobDetail.getKey());
                    return getSchedulerJobDetail(jobDetail.getKey());
                }
                else {
                    logger.info("Job schedule update validation checks failed for ID: {}", jobId);
                    logger.error("Job update validation check failed: {}", jobRequest.getExceptions());
                    throw new Exception("Job update validation checks failed");
                }
            }
            catch (SchedulerException e) {
                logger.error("Scheduler exception while getting job with Id: {}", jobId);
                throw e;
            }
        }

        logger.error("Job with Id: {} not found", jobId);
        throw new Exception("Job not found for ID");
    }

    @Override
    public SchedulerJobDetail getJob(String product, String jobGroup, UUID jobId) throws Exception {
        if (isJobPresent(product, jobGroup, jobId)) {
            try {
                JobDetail jobDetail = scheduler.getJobDetail(
                        jobKey(jobId.toString(), Utils.getJobGroupId(product, jobGroup)));
                return getSchedulerJobDetail(jobDetail.getKey());
            }
            catch (SchedulerException e) {
                logger.error("Scheduler exception while getting job with Id: {}", jobId);
                throw e;
            }
        }

        logger.error("Job with Id: {} not found", jobId);
        throw new Exception("Not found");
    }

    @Override
    public void deleteJob(String product, String jobGroup, UUID jobId) throws Exception {
        if (isJobPresent(product, jobGroup, jobId)) {
            scheduler.deleteJob(jobKey(jobId.toString(), Utils.getJobGroupId(product, jobGroup)));
        }
        else {
            logger.error("Scheduler exception while deleting job with ID: {}", jobId);
            throw new Exception("SchedulerJobDetail doesn't exist");
        }
    }

    @Override
    public void startScheduler() throws Exception {
        scheduler.start();
    }

    @Override
    public void stopScheduler() throws Exception {
        scheduler.shutdown();
    }

    @Override
    public void registerJobClassForJobGroup(String jobGroupId, Class<? extends SchedulerJob> jobClass) {
        jobGroupToClassMapping.put(jobGroupId, jobClass);
    }

    @Override
    public void registerTriggerListenerForJobGroup(String jobGroupId, SchedulerTriggerListener triggerListener)
            throws Exception {
        scheduler.getListenerManager().addTriggerListener(triggerListener, triggerGroupEquals(jobGroupId));
    }

    @Override
    public void pauseJob(String product, String jobGroup, UUID jobId) throws Exception {
        scheduler.pauseJob(jobKey(jobId.toString(), Utils.getJobGroupId(product, jobGroup)));
    }

    @Override
    public void resumeJob(String product, String jobGroup, UUID jobId) throws Exception {
        scheduler.resumeJob(jobKey(jobId.toString(), Utils.getJobGroupId(product, jobGroup)));
    }
}
