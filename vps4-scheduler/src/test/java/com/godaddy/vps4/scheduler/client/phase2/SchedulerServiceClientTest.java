package com.godaddy.vps4.scheduler.client.phase2;

import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.scheduler.core.JobRequest;
import com.godaddy.vps4.scheduler.web.client.SchedulerServiceClientModule;
import com.godaddy.vps4.scheduler.core.JobType;
import com.godaddy.vps4.scheduler.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.core.config.ConfigModule;
import com.godaddy.vps4.scheduler.plugin.backups.Vps4BackupJob;
import com.godaddy.vps4.scheduler.web.client.SchedulerService;
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
import java.util.UUID;

import static org.junit.Assert.fail;


/**
 * This is not a unit test. The test methods in this class actually exercise the code and not just the behaviour.
 * When using an IDE, run the Vps4SchedulerMain in a separate run configuration.
 * Run this test class as a separate run configuration.
 * The tests in this class will require the scheduler server to be running.
 * The scheduler server can also be run from the command line and the tests can then be run against the server.
 */
@Ignore
public class SchedulerServiceClientTest {

    private Injector injector;
    @Inject @ClientCertAuth SchedulerService schedulerService;
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
            schedulerService.deleteJob(product, jobGroup, jobId);
        }
        catch (Exception e) {
        }
    }

    private SchedulerJobDetail createJob() {
        Vps4BackupJob.Request request = new Vps4BackupJob.Request();
        request.vmId = vmId;
        request.when = Instant.now().plusSeconds(180);
        request.jobType = JobType.ONE_TIME;
        SchedulerJobDetail jobDetail = schedulerService.submitJobToGroup(product, jobGroup, request);
        jobId = jobDetail.id;

        return jobDetail;
    }

    @Test
    public void getGroupJobs() throws Exception {
        Assert.assertEquals(0, schedulerService.getGroupJobs(product, jobGroup).size());
    }

    @Test
    public void submitJobToGroup() throws Exception {
        Vps4BackupJob.Request request = new Vps4BackupJob.Request();
        request.vmId = vmId;
        request.when = Instant.now().plusSeconds(180);
        request.jobType = JobType.ONE_TIME;
        SchedulerJobDetail jobDetail = schedulerService.submitJobToGroup(product, jobGroup, request);
        jobId = jobDetail.id;

        Assert.assertEquals(vmId, request.vmId);
    }

    @Test
    public void submitJobBeforeLeadTimeFails() throws Exception {
        Vps4BackupJob.Request request = new Vps4BackupJob.Request();
        request.vmId = vmId;
        request.when = Instant.now().plusSeconds(JobRequest.JOB_SCHEDULE_LEAD_TIME_WINDOW - 1);
        request.jobType = JobType.ONE_TIME;
        try {
            schedulerService.submitJobToGroup(product, jobGroup, request);
        }
        catch (ClientErrorException e) {
            Assert.assertEquals(Response.Status.CONFLICT.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    public void getJob() throws Exception {
        SchedulerJobDetail jobDetail = createJob();
        Assert.assertEquals(
                jobDetail.nextRun, schedulerService.getJob(product, jobGroup, jobDetail.id).nextRun);
    }

    @Test(expected = NotFoundException.class)
    public void getNonExistentJobFails() throws Exception {
        schedulerService.getJob(product, jobGroup, UUID.randomUUID());
    }

    @Test
    public void updateJob() throws Exception {
        SchedulerJobDetail jobDetail = createJob();

        Vps4BackupJob.Request modifyRequest = new Vps4BackupJob.Request();
        modifyRequest.vmId = vmId;
        modifyRequest.when = Instant.now().plusSeconds(120);
        modifyRequest.jobType = JobType.ONE_TIME;
        Assert.assertEquals(modifyRequest.when, schedulerService.rescheduleJob(product, jobGroup, jobDetail.id, modifyRequest).nextRun);
    }

    @Test
    public void deleteJob() throws Exception {
        Vps4BackupJob.Request request = new Vps4BackupJob.Request();
        request.vmId = UUID.randomUUID();
        request.when = Instant.now().plusSeconds(180);
        request.jobType = JobType.ONE_TIME;
        SchedulerJobDetail jobDetail = schedulerService.submitJobToGroup(product, jobGroup, request);

        try {
            schedulerService.deleteJob("vps4", "backups", jobDetail.id);
        }
        catch (Exception e) {
            fail("Deletion failed");
        }
    }
}