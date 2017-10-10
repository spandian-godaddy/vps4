package com.godaddy.vps4.scheduler.core.quartz;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.impl.matchers.GroupMatcher.jobGroupEquals;
import static org.quartz.impl.matchers.GroupMatcher.triggerGroupEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.scheduler.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.core.JobRequest;
import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.utils.Utils;
import com.google.inject.Inject;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class QuartzSchedulerService implements SchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(QuartzSchedulerService.class);
    private static final int JOB_SCHEDULE_LEAD_TIME_WINDOW = 60; // 60 seconds

    private final Scheduler scheduler;
    private final ObjectMapper objectMapper;
    private static final Map<String, Class<? extends Job>> jobGroupToClassMapping;

    static {
       jobGroupToClassMapping = new HashMap<>();
    }

    @Inject
    public QuartzSchedulerService(Scheduler scheduler, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
    }

    private Class<? extends JobRequest> getJobRequestClass(Class<? extends Job> jobClass) throws Exception {
        Class<?>[] declaredClasses = jobClass.getDeclaredClasses();

        @SuppressWarnings("unchecked")
        List<Class<JobRequest>> jobRequestClasses
            = Arrays.stream(declaredClasses)
                .filter(JobRequest.class::isAssignableFrom)
                .map(o -> (Class<JobRequest>)o)
                .collect(Collectors.toList());

        if (!jobRequestClasses.isEmpty())
            return jobRequestClasses.get(0);

        throw new Exception("No job request class present");
    }

    private JobDetail buildJob(String groupId, Class<? extends Job> jobClass, String jobDataJson)
            throws Exception {
        String jobId = UUID.randomUUID().toString();
        JobBuilder jobBuilder = newJob(jobClass).withIdentity(jobId, groupId);
        jobBuilder.usingJobData("jobDataJson", jobDataJson);
        jobBuilder.usingJobData("jobRequestClass", getJobRequestClass(jobClass).getName());
        return jobBuilder.build();
    }

    private Trigger buildTrigger(String groupName, Instant when) {
        String triggerName = UUID.randomUUID().toString();
        return newTrigger()
           .withIdentity(triggerName, groupName)
           .startAt(Date.from(when))
           .withSchedule(simpleSchedule()
               .withRepeatCount(0)
               .withMisfireHandlingInstructionFireNow())  // Instructs the Scheduler that upon a mis-fire situation, the trigger wants to be fired now by Scheduler.
           .build();
    }

    private SchedulerJobDetail scheduleJob(String product, String jobGroup,
                                           Class<? extends Job> jobClass,
                                           String jobDataJson,
                                           Instant when)
        throws Exception
    {
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        JobDetail jobDetail = buildJob(jobGroupId, jobClass, jobDataJson);
        Trigger trigger = buildTrigger(jobGroupId, when);
        scheduler.scheduleJob(jobDetail, trigger);

        return getSchedulerJobDetail(jobDetail.getKey());
    }

    private SchedulerJobDetail getSchedulerJobDetail(JobKey jobKey) throws SchedulerException {
        Trigger trigger = getExistingTriggerForJob(jobKey);
        Instant when = trigger.getNextFireTime().toInstant();
        return new SchedulerJobDetail(UUID.fromString(jobKey.getName()), when);
    }

    private boolean isJobPresent(String product, String jobGroup, UUID jobId) throws SchedulerException {
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        return scheduler.checkExists(new JobKey(jobId.toString(), jobGroupId));
    }

    private boolean hasJobClassBeenRegisteredForJobGroup(String jobGroupId) {
        return jobGroupToClassMapping.containsKey(jobGroupId);
    }

    private Class<? extends Job> getJobClassForGroup(String jobGroupId) {
        return jobGroupToClassMapping.get(jobGroupId);
    }

    private Trigger getExistingTriggerForJob(JobKey jobKey) throws SchedulerException {
        return scheduler.getTriggersOfJob(jobKey).get(0);
    }

    private boolean validateJobSchedule(Instant when) {
        return when.isAfter(Instant.now().plusSeconds(JOB_SCHEDULE_LEAD_TIME_WINDOW));
    }

    private void replaceJobTrigger(Instant toWhen, String jobGroupId, JobKey jobKey) throws SchedulerException {
        Trigger oldTrigger = getExistingTriggerForJob(jobKey);
        Trigger newTrigger = buildTrigger(jobGroupId, toWhen);
        scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger);
    }

    @Override
    public SchedulerJobDetail createJob(String product, String jobGroup, String requestJson) throws Exception {
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        if (hasJobClassBeenRegisteredForJobGroup(jobGroupId)) {
            JobRequest jobRequest = objectMapper.readValue(requestJson, JobRequest.class);
            if (validateJobSchedule(jobRequest.when)) {
                Class<? extends Job> jobClass = getJobClassForGroup(jobGroupId);
                return scheduleJob(product, jobGroup, jobClass, requestJson, jobRequest.when);
            }
            else {
                logger.info("Job creation validation checks failed");
                throw new Exception(
                    String.format("Job can be scheduled to run only after: %s",
                        Instant.now().plusSeconds(JOB_SCHEDULE_LEAD_TIME_WINDOW)));
            }
        }

        throw new Exception("Couldn't create job");
    }

    @Override
    public List<SchedulerJobDetail> getGroupJobs(String product, String jobGroup) {
        List<SchedulerJobDetail> jobs = new ArrayList<>();
        try {
            for (JobKey jobKey: scheduler.getJobKeys(jobGroupEquals(Utils.getJobGroupId(product, jobGroup)))) {
                SchedulerJobDetail schedulerJobDetail = getSchedulerJobDetail(jobKey);
                jobs.add(schedulerJobDetail);
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
                validateJobSchedule(jobRequest.when);
                if (validateJobSchedule(jobRequest.when)) {
                    String jobGroupId = Utils.getJobGroupId(product, jobGroup);
                    JobDetail jobDetail = scheduler.getJobDetail(
                            jobKey(jobId.toString(), Utils.getJobGroupId(product, jobGroup)));
                    replaceJobTrigger(jobRequest.when, jobGroupId, jobDetail.getKey());
                    return getSchedulerJobDetail(jobDetail.getKey());
                }
                else {
                    logger.info("Job schedule update validation checks failed for ID: {}", jobId);
                    throw new Exception(String.format("Job can be scheduled to run only after: %s",
                        Instant.now().plusSeconds(JOB_SCHEDULE_LEAD_TIME_WINDOW)));
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
    public void registerJobClassForJobGroup(String jobGroupId, Class<? extends Job> jobClass) {
        jobGroupToClassMapping.put(jobGroupId, jobClass);
    }

    @Override
    public void registerTriggerListenerForJobGroup(String jobGroupId, SchedulerTriggerListener triggerListener)
            throws Exception {
        scheduler.getListenerManager().addTriggerListener(triggerListener, triggerGroupEquals(jobGroupId));
    }
}
