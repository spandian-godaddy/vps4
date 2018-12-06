package com.godaddy.vps4.scheduler.core.quartz;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.impl.matchers.GroupMatcher.anyGroup;
import static org.quartz.TriggerBuilder.newTrigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.Required;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.godaddy.vps4.scheduler.core.quartz.memory.QuartzMemoryModule;
import com.godaddy.vps4.scheduler.core.utils.Utils;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class QuartzSchedulerServiceRESTTest {

    private static final Logger logger = LoggerFactory.getLogger(QuartzSchedulerServiceRESTTest.class);
    static Injector injector;

    @Inject private SchedulerService schedulerService;
    @Inject private Scheduler scheduler;
    @Inject private ObjectMapper objectMapper;
    String product;
    String jobGroup;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new ObjectMapperModule(),
                new QuartzMemoryModule(),
                new QuartzModule()
        );
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        product = "vps4";
        jobGroup = "backups";
        schedulerService.registerJobClassForJobGroup(Utils.getJobGroupId(product, jobGroup), TestJob.class);
    }

    @After
    public void tearDown() throws Exception {
        List<JobKey> jobKeys = new ArrayList<>(scheduler.getJobKeys(anyGroup()));
        scheduler.deleteJobs(jobKeys);
    }

    private JobDetail buildJob(String groupId, Class<? extends Job> jobClass)
            throws Exception {
        String jobId = UUID.randomUUID().toString();
        JobBuilder jobBuilder = newJob(jobClass).withIdentity(jobId, groupId);
        String jobDataJson = getJobRequestData();
        jobBuilder.usingJobData("jobDataJson", jobDataJson);
        return jobBuilder.build();
    }

    private String getJobRequestData() throws JsonProcessingException {
        JobRequest request = new JobRequest();
        request.when = Instant.now().plusSeconds(180);
        request.jobType = JobType.ONE_TIME;
        return objectMapper.writeValueAsString(request);
    }

    private Trigger buildTrigger(String groupName) {
        Instant when = Instant.now().plusSeconds(15);
        String triggerName = UUID.randomUUID().toString();
        return newTrigger()
                .withIdentity(triggerName, groupName)
                .startAt(Date.from(when))
                .withSchedule(simpleSchedule()
                        .withRepeatCount(0))
                .build();
    }

    private JobKey scheduleJob() {
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        try {
            JobDetail jobDetail = buildJob(jobGroupId, TestJob.class);
            Trigger trigger = buildTrigger(jobGroupId);
            scheduler.scheduleJob(jobDetail, trigger);
            return jobDetail.getKey();
        }
        catch (Exception e) {
            logger.error("error scheduling job");
            fail("Error!!");
            return null;
        }
    }

    private void scheduleJobs(int howMany) {
        IntStream.range(0, howMany).forEach(i -> this.scheduleJob());
    }

    @Test
    public void getJobsForGroupWhenNoJobsCurrentlyScheduled() {
        Assert.assertEquals(0, schedulerService.getGroupJobs(product, jobGroup).size());
    }

    @Test
    public void getJobsForGroupWhenJobsExist() {
        scheduleJobs(10);
        Assert.assertEquals(10, schedulerService.getGroupJobs(product, jobGroup).size());
    }

    @Test
    public void schedulingJobForAfterLeadTimeWindowSucceeds() {
        TestJob.TestRequest request = new TestJob.TestRequest();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(180); // make sure we are outside the window
        request.jobType = JobType.ONE_TIME;
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            schedulerService.createJob(product, jobGroup, requestJson);
            Assert.assertEquals(1, schedulerService.getGroupJobs(product, jobGroup).size());
        }
        catch (Exception e) {
            fail("Error job creation");
        }
    }

    @Test(expected = Exception.class)
    public void schedulingJobForBeforeLeadTimeWindowFails() throws Exception {
        TestJob.TestRequest request = new TestJob.TestRequest();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(30);
        request.jobType = JobType.ONE_TIME;
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            schedulerService.createJob(product, jobGroup, requestJson);
        }
        catch (JsonProcessingException e) {
            fail("Error job creation");
        }
    }

    @Test
    public void schedulingRecurringJobSucceeds() {
        TestJob.TestRequest request = new TestJob.TestRequest();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(180); // make sure we are outside the window
        request.jobType = JobType.RECURRING;
        request.repeatIntervalInDays = 1;
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            schedulerService.createJob(product, jobGroup, requestJson);
            Assert.assertEquals(1, schedulerService.getGroupJobs(product, jobGroup).size());
        }
        catch (Exception e) {
            fail("Error job creation");
        }
    }

    @Test
    public void gettingAnExistingJobSucceeds() {
        JobKey jobKey = scheduleJob();
        try {
            SchedulerJobDetail jobDetail = schedulerService.getJob(product, jobGroup, UUID.fromString(jobKey.getName()));
            Assert.assertEquals(
                    jobKey.getName(),
                    jobDetail.id.toString());
            Assert.assertEquals(JobType.ONE_TIME, jobDetail.jobRequest.jobType); // We are only defining one time jobs in this test
        }
        catch (Exception e) {
            fail("There should be no exception!!");
        }
    }

    @Test
    public void pauseAndResumeJobSetsJobState() {
        JobKey jobKey = scheduleJob();
        try {
            schedulerService.pauseJob(product, jobGroup, UUID.fromString(jobKey.getName()));
            SchedulerJobDetail jobDetail = schedulerService.getJob(product, jobGroup, UUID.fromString(jobKey.getName()));

            Assert.assertEquals(true, jobDetail.isPaused);

            schedulerService.resumeJob(product, jobGroup, UUID.fromString(jobKey.getName()));
            jobDetail = schedulerService.getJob(product, jobGroup, UUID.fromString(jobKey.getName()));

            Assert.assertEquals(false, jobDetail.isPaused);

        }
        catch (Exception e) {
            fail("There should be no exception!!");
        }
    }

    @Test(expected = Exception.class)
    public void gettingANonExistentJobFails() throws Exception {
        schedulerService.getJob(product, jobGroup, UUID.randomUUID());
    }

    @Test
    public void reschedulingJobForAfterLeadTimeWindowSucceeds() {
        JobKey jobKey = scheduleJob();
        TestJob.TestRequest request = new TestJob.TestRequest();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(180); // make sure we are outside the window
        request.jobType = JobType.ONE_TIME;
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            schedulerService.updateJobSchedule(product, jobGroup, UUID.fromString(jobKey.getName()), requestJson);
            Assert.assertEquals(1, schedulerService.getGroupJobs(product, jobGroup).size());
        }
        catch (Exception e) {
            fail("Error while updating job schedule");
        }
    }

    @Test(expected = Exception.class)
    public void reschedulingJobForBeforeLeadTimeWindowFails() throws Exception {
        JobKey jobKey = scheduleJob();
        TestJob.TestRequest request = new TestJob.TestRequest();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(30);
        request.jobType = JobType.ONE_TIME;
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            schedulerService.updateJobSchedule(product, jobGroup, UUID.fromString(jobKey.getName()), requestJson);
        }
        catch (JsonProcessingException e) {
            fail("Error job creation");
        }
    }

    @Test
    public void deletingAnExistingJobSucceeds() {
        JobKey jobKey = scheduleJob();
        try {
            schedulerService.deleteJob(product, jobGroup, UUID.fromString(jobKey.getName()));
            Assert.assertEquals(0, schedulerService.getGroupJobs(product, jobGroup).size());
        }
        catch (Exception e) {
            fail("There should be no exception!!");
        }
    }

    @Test(expected = Exception.class)
    public void deletingANonExistentJobFails() throws Exception {
        schedulerService.deleteJob(product, jobGroup, UUID.randomUUID());
    }

    @JobMetadata(
        product = "vps4",
        jobGroup = "backups",
        jobRequestType = TestJob.TestRequest.class
    )
    public static class TestJob extends SchedulerJob {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        }

        public static class TestRequest extends JobRequest {
            @Required public UUID vmId;
        }
    }
}