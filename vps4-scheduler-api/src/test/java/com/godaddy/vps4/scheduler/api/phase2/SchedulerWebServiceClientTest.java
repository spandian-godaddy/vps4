package com.godaddy.vps4.scheduler.api.phase2;

import static com.godaddy.vps4.client.ClientUtils.withShopperId;

import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.scheduler.api.client.SchedulerServiceClientModule;
import com.godaddy.vps4.scheduler.api.core.JobRequest;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.plugin.Vps4BackupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


/**
 * This is not a unit test. The test methods in this class actually exercise the code and not just the behaviour.
 * When using an IDE, run the Vps4SchedulerMain in a separate run configuration.
 * Run this test class as a separate run configuration.
 * The tests in this class will require the scheduler server to be running.
 * The scheduler server can also be run from the command line and the tests can then be run against the server.
 */
@Ignore
public class SchedulerWebServiceClientTest {

    private Injector injector;
    @Inject @ClientCertAuth
    SchedulerWebService schedulerWebService;
    private final String product = "vps4";
    private final String jobGroup = "backups";
    private UUID jobId;
    private final UUID vmId = UUID.randomUUID();

    @Before
    public void setUp() throws Exception {
        injector = Guice.createInjector(
                new ConfigModule(),
                new ObjectMapperModule(),
                new SchedulerServiceClientModule()
        );

        injector.injectMembers(this);
    }

    @After
    public void tearDown() throws Exception {
        try {
            schedulerWebService.deleteJob(product, jobGroup, jobId);
        }
        catch (Exception e) {
        }
    }

    private SchedulerJobDetail createJob() {
        Vps4BackupJobRequest request = new Vps4BackupJobRequest();
        request.vmId = vmId;
        request.backupName = "backup123";
        request.when = Instant.now().plusSeconds(180);
        request.jobType = JobType.ONE_TIME;
        SchedulerJobDetail jobDetail = schedulerWebService.submitJobToGroup(product, jobGroup, request);
        jobId = jobDetail.id;

        return jobDetail;
    }

    @Test
    public void getGroupJobs() throws Exception {
        Assert.assertEquals(0, schedulerWebService.getGroupJobs(product, jobGroup).size());
    }

    @Test
    public void getGroupJobsWithShopperIdInjected() throws Exception {
        // This test just shows shopper id being injected into a request being made to the scheduler service.
        // Scheduler has no concept of shoppers or roles, so this test servers merely as an example for shopper id
        // injection into a request that uses client cert auth.
        @SuppressWarnings("unchecked")
        List<SchedulerJobDetail> groupJobs = withShopperId("959998", () -> {
            return schedulerWebService.getGroupJobs(product, jobGroup);
        }, List.class);
        Assert.assertEquals(0, groupJobs.size());
    }

    @Test
    public void submitJobToGroup() throws Exception {
        Vps4BackupJobRequest request = new Vps4BackupJobRequest();
        request.vmId = vmId;
        request.backupName = "backup123";
        request.when = Instant.now().plusSeconds(180);
        request.jobType = JobType.ONE_TIME;
        SchedulerJobDetail jobDetail = schedulerWebService.submitJobToGroup(product, jobGroup, request);
        jobId = jobDetail.id;

        Assert.assertEquals(vmId, request.vmId);
    }

    @Test
    public void submitJobBeforeLeadTimeFails() throws Exception {
        Vps4BackupJobRequest request = new Vps4BackupJobRequest();
        request.vmId = vmId;
        request.backupName = "backup123";
        request.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW - 1);
        request.jobType = JobType.ONE_TIME;
        try {
            schedulerWebService.submitJobToGroup(product, jobGroup, request);
        }
        catch (ClientErrorException e) {
            Assert.assertEquals(Response.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void getJob() throws Exception {
        SchedulerJobDetail jobDetail = createJob();
        Assert.assertEquals(
                jobDetail.nextRun, schedulerWebService.getJob(product, jobGroup, jobDetail.id).nextRun);
    }

    @Test
    public void getJobWithShopperIdInjected() throws Exception {
        SchedulerJobDetail jobDetail = createJob();

        @SuppressWarnings("unchecked")
        SchedulerJobDetail getJobDetail = withShopperId("959998", () -> {
            return schedulerWebService.getJob(product, jobGroup, jobDetail.id);
        }, SchedulerJobDetail.class);
        Assert.assertEquals(jobDetail.nextRun, getJobDetail.nextRun);
    }

    @Test(expected = NotFoundException.class)
    public void getNonExistentJobFails() throws Exception {
        schedulerWebService.getJob(product, jobGroup, UUID.randomUUID());
    }

    @Test
    public void updateJob() throws Exception {
        SchedulerJobDetail jobDetail = createJob();

        Vps4BackupJobRequest modifyRequest = new Vps4BackupJobRequest();
        modifyRequest.vmId = vmId;
        modifyRequest.backupName = "backup123";
        modifyRequest.when = Instant.now().plusSeconds(120);
        modifyRequest.jobType = JobType.ONE_TIME;
        Assert.assertEquals(modifyRequest.when, schedulerWebService.rescheduleJob(product, jobGroup, jobDetail.id, modifyRequest).nextRun);
    }

    @Test
    public void deleteJob() throws Exception {
        Vps4BackupJobRequest request = new Vps4BackupJobRequest();
        request.vmId = UUID.randomUUID();
        request.backupName = "backup123";
        request.when = Instant.now().plusSeconds(180);
        request.jobType = JobType.ONE_TIME;
        SchedulerJobDetail jobDetail = schedulerWebService.submitJobToGroup(product, jobGroup, request);

        try {
            schedulerWebService.deleteJob("vps4", "backups", jobDetail.id);
        }
        catch (Exception e) {
            Assert.fail("Deletion failed");
        }
    }
}