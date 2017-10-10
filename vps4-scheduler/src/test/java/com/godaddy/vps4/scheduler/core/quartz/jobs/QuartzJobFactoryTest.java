package com.godaddy.vps4.scheduler.core.quartz.jobs;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.quartz.JobBuilder.newJob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.scheduler.core.JobRequest;
import com.godaddy.vps4.scheduler.core.quartz.QuartzJobFactory;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;

import java.time.Instant;
import java.util.UUID;

public class QuartzJobFactoryTest {

    Injector injector;
    TriggerFiredBundle mockTriggerFiredBundle;
    Scheduler mockScheduer;

    @Inject private QuartzJobFactory quartzJobFactory;
    @Inject private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        injector = Guice.createInjector(
                new ObjectMapperModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(QuartzJobFactory.class);
                    }
                }
        );
        injector.injectMembers(this);
        mockScheduer = mock(Scheduler.class);
        mockTriggerFiredBundle = mock(TriggerFiredBundle.class);
    }

    private String getRequestJson(JobRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException e) {
            fail("Error job creation");
            return null;
        }
    }

    private JobBuilder buildJobHelper(Class<? extends Job> jobClass) {
        String groupId = UUID.randomUUID().toString();
        String jobId = UUID.randomUUID().toString();
        return newJob(jobClass).withIdentity(jobId, groupId);
    }

    private JobDetail buildJob(Class<? extends Job> jobClass,
                               Class<? extends JobRequest> jobRequestClass,
                               String jobDataJson) {
        JobBuilder jobBuilder = buildJobHelper(jobClass);

        if (jobDataJson != null) {
            jobBuilder.usingJobData("jobDataJson", jobDataJson);
        }

        if (jobRequestClass != null) {
            jobBuilder.usingJobData("jobRequestClass", jobRequestClass.getName());
        }

        return jobBuilder.build();
    }

    private JobDetail buildJob(Class<? extends Job> jobClass,
                               String jobDataJson) {
        return buildJob(jobClass, null, jobDataJson);
    }

    private JobDetail buildJob(Class<? extends Job> jobClass, Class<? extends JobRequest> jobRequestClass) {
        return buildJob(jobClass, jobRequestClass, null);
    }

    private JobDetail buildJob(Class<? extends Job> jobClass) {
        return buildJob(jobClass, null, null);
    }

    @Test
    public void createsNewJobAndSetsRequestField() throws Exception {
        JobClassWithRequestAndSetter.Request request = new JobClassWithRequestAndSetter.Request();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(180);
        String jobRequestJson = getRequestJson(request);
        when(mockTriggerFiredBundle.getJobDetail())
            .thenReturn(buildJob(JobClassWithRequestAndSetter.class, JobClassWithRequestAndSetter.Request.class, jobRequestJson));

        Job job = quartzJobFactory.newJob(mockTriggerFiredBundle, mockScheduer);
        Assert.assertTrue(JobClassWithRequestAndSetter.class.isInstance(job));

        Assert.assertEquals(((JobClassWithRequestAndSetter) job).request.vmId, request.vmId);
        Assert.assertEquals(((JobClassWithRequestAndSetter) job).request.when, request.when);
    }

    @Test
    public void createsNewJobEvenWhenJobRequestDataNotProvided() throws Exception {
        when(mockTriggerFiredBundle.getJobDetail()).thenReturn(buildJob(JobClassWithNoRequest.class));

        Job job = quartzJobFactory.newJob(mockTriggerFiredBundle, mockScheduer);
        Assert.assertTrue(JobClassWithNoRequest.class.isInstance(job));
    }

    @Test(expected = SchedulerException.class)
    public void jobCreationWithRequestDataButNoJobRequestClassThrowsException() throws Exception {
        JobClassWithRequestAndSetter.Request request = new JobClassWithRequestAndSetter.Request();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(180);
        String jobRequestJson = getRequestJson(request);
        when(mockTriggerFiredBundle.getJobDetail())
                .thenReturn(buildJob(JobClassWithRequestAndSetter.class, jobRequestJson));

        Job job = quartzJobFactory.newJob(mockTriggerFiredBundle, mockScheduer);
    }

    @Test(expected = SchedulerException.class)
    public void jobCreationWithRequestDataButNoSetterThrowsException() throws Exception {
        JobClassWithNoSetter.Request request = new JobClassWithNoSetter.Request();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(180);
        String jobRequestJson = getRequestJson(request);
        when(mockTriggerFiredBundle.getJobDetail()).thenReturn(buildJob(JobClassWithNoSetter.class, jobRequestJson));

        Job job = quartzJobFactory.newJob(mockTriggerFiredBundle, mockScheduer);
    }
}