package com.godaddy.vps4.orchestration.scheduler;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;

import gdg.hfs.orchestration.CommandContext;

@RunWith(value = MockitoJUnitRunner.class)
public class RescheduleZombieVmCleanupTest {

    private SchedulerWebService schedulerWebService = mock(SchedulerWebService.class);
    private SchedulerJobDetail schedulerJobDetail = mock(SchedulerJobDetail.class);
    private CommandContext context = mock(CommandContext.class);
    private RescheduleZombieVmCleanup.Request rescheduleZombieVmCleanupRequest;
    private UUID jobId = UUID.randomUUID();
    private UUID vmId = UUID.randomUUID();
    private Instant when = Instant.now().plus(7, ChronoUnit.DAYS);
    @Captor
    private ArgumentCaptor<Function<CommandContext, SchedulerJobDetail>> schedulerWebServiceArgCaptor;
    @Captor
    private ArgumentCaptor<Vps4ZombieCleanupJobRequest> jobRequestArgumentCaptor;

    private RescheduleZombieVmCleanup rescheduleZombieVmCleanupCommand;

    @Before
    public void setUp() throws Exception {
        rescheduleZombieVmCleanupRequest = new RescheduleZombieVmCleanup.Request();
        rescheduleZombieVmCleanupRequest.jobId = jobId;
        rescheduleZombieVmCleanupRequest.vmId = vmId;
        rescheduleZombieVmCleanupRequest.when = when;
        when(context.execute(eq("Reschedule Cleanup Zombie VM"), any(Function.class), eq(SchedulerJobDetail.class)))
                .thenReturn(schedulerJobDetail);
        rescheduleZombieVmCleanupCommand = new RescheduleZombieVmCleanup(schedulerWebService);
    }

    @Test
    public void invokesRescheduleJob() {
        when(schedulerWebService.rescheduleJob(eq("vps4"), eq("zombie"), eq(jobId),
                any(Vps4ZombieCleanupJobRequest.class))).thenReturn(schedulerJobDetail);

        rescheduleZombieVmCleanupCommand.execute(context, rescheduleZombieVmCleanupRequest);

        // verify call to the scheduler service is wrapped in a context.execute method
        verify(context, times(1))
                .execute(eq("Reschedule Cleanup Zombie VM"), schedulerWebServiceArgCaptor.capture(),
                        eq(SchedulerJobDetail.class));

        // Verify that the lambda is calling the appropriate scheduler service method
        Function<CommandContext, SchedulerJobDetail> lambda = schedulerWebServiceArgCaptor.getValue();
        lambda.apply(context);
        verify(schedulerWebService, times(1))
                .rescheduleJob(eq("vps4"), eq("zombie"), eq(jobId), jobRequestArgumentCaptor.capture());

        // verify the job creation payload data
        Vps4ZombieCleanupJobRequest jobRequestData = jobRequestArgumentCaptor.getValue();
        Assert.assertEquals(vmId, jobRequestData.vmId);
        Assert.assertEquals(JobType.ONE_TIME, jobRequestData.jobType);
        Assert.assertEquals(rescheduleZombieVmCleanupRequest.when, jobRequestData.when);
    }

    @Test(expected = RuntimeException.class)
    public void throwsExceptionIfRescheduleVmDeleteFails() {
        when(schedulerWebService.rescheduleJob(eq("vps4"), eq("zombie"), eq(jobId),
                any(Vps4ZombieCleanupJobRequest.class))).thenThrow(
                new Exception("Error while re-scheduling zombie cleanup job for VM"));

        rescheduleZombieVmCleanupCommand.execute(context, rescheduleZombieVmCleanupRequest);
    }
}