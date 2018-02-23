package com.godaddy.vps4.scheduler.api.core;

import java.time.Instant;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.godaddy.vps4.scheduler.api.core.jobRequests.JobRequestOne;

public class JobRequestTest {

    @Test
    public void jobRequestWithoutJobTypeFails() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("REQD_FIELD_MISSING", exceptions.get(0).getId());
        Assert.assertTrue(exceptions.get(0).getMessage().contains("'jobType'"));
    }

    @Test
    public void jobRequestWithoutStartTimeFails() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.jobType = JobType.ONE_TIME;

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("REQD_FIELD_MISSING", exceptions.get(0).getId());
        Assert.assertTrue(exceptions.get(0).getMessage().contains("'when'"));
    }

    @Test
    public void oneTimeJobAfterLeadTimeWindowSucceeds() throws Exception {
       JobRequest jobRequest = new JobRequest();
       jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
       jobRequest.jobType = JobType.ONE_TIME;

       Assert.assertTrue(jobRequest.isValid());
       Assert.assertEquals(0, jobRequest.getExceptions().size());
    }

    @Test
    public void oneTimeJobBeforeLeadTimeWindowFails() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW);
        jobRequest.jobType = JobType.ONE_TIME;

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("INVALID_START_TIME", exceptions.get(0).getId());
    }

    @Test
    public void recurringJobForIndefiniteRunAfterLeadTimeSucceeds() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.RECURRING;
        jobRequest.repeatIntervalInDays = 7;

        Assert.assertTrue(jobRequest.isValid());
        Assert.assertEquals(0, jobRequest.getExceptions().size());
    }

    @Test
    public void recurringJobForIndefiniteRunBeforeLeadTimeFails() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW);
        jobRequest.jobType = JobType.RECURRING;
        jobRequest.repeatIntervalInDays = 7;

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("INVALID_START_TIME", exceptions.get(0).getId());
    }

    @Test
    public void recurringJobForIndefiniteRunWithoutRepeatIntervalFails() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.RECURRING;

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("INVALID_REPEAT_INTERVAL", exceptions.get(0).getId());
    }

    @Test
    public void recurringJobForIndefiniteRunWithInvalidRepeatIntervalFails() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.RECURRING;
        jobRequest.repeatIntervalInDays = 0;

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("INVALID_REPEAT_INTERVAL", exceptions.get(0).getId());
    }

    @Test
    public void recurringJobForFiniteRunAfterLeadTimeSucceeds() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.RECURRING;
        jobRequest.repeatIntervalInDays = 7;
        jobRequest.repeatCount = 2;

        Assert.assertTrue(jobRequest.isValid());
        Assert.assertEquals(0, jobRequest.getExceptions().size());
    }

    @Test
    public void recurringJobForFiniteRunWithInvalidRepeatCountFails() throws Exception {
        JobRequest jobRequest = new JobRequest();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.RECURRING;
        jobRequest.repeatIntervalInDays = 1;
        jobRequest.repeatCount = 0;

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("INVALID_REPEAT_COUNT", exceptions.get(0).getId());
    }

    @Test
    public void pluginJobRequestWithValidValuesSucceeds() throws Exception {
        JobRequestOne jobRequest = new JobRequestOne();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.ONE_TIME;
        jobRequest.jobParamOne = 1;
        jobRequest.jobParamThree = "foobar";

        Assert.assertTrue(jobRequest.isValid());
        Assert.assertEquals(0, jobRequest.getExceptions().size());
    }

    @Test
    public void pluginJobRequestWithMissingRequiredFieldFails() throws Exception {
        JobRequestOne jobRequest = new JobRequestOne();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.ONE_TIME;

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("REQD_FIELD_MISSING", exceptions.get(0).getId());
    }

    @Test
    public void pluginJobRequestWithInvalidRequiredFieldFails() throws Exception {
        JobRequestOne jobRequest = new JobRequestOne();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.ONE_TIME;
        jobRequest.jobParamOne = 2;

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("INVALID_PARAM_ONE", exceptions.get(0).getId());
    }

    @Test
    public void pluginJobRequestWithInvalidOptionalFieldFails() throws Exception {
        JobRequestOne jobRequest = new JobRequestOne();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.ONE_TIME;
        jobRequest.jobParamOne = 1;
        jobRequest.jobParamThree = "hello";

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals(1, exceptions.size());
        Assert.assertEquals("INVALID_PARAM_THREE", exceptions.get(0).getId());
    }

    @Test
    public void pluginJobRequestInvalidObjectLevelCheck() throws Exception {
        JobRequestOne jobRequest = new JobRequestOne();
        jobRequest.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW + 1);
        jobRequest.jobType = JobType.ONE_TIME;
        jobRequest.jobParamOne = 3;
        jobRequest.jobParamThree = "fail";

        Assert.assertFalse(jobRequest.isValid());
        List<Vps4JobRequestValidationException> exceptions = jobRequest.getExceptions();
        Assert.assertEquals("OBJ_LVL_FAIL", exceptions.get(0).getId());
    }
}