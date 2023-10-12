package com.godaddy.vps4.orchestration.vm.rebuild;

import com.godaddy.vps4.orchestration.scheduler.DeleteScheduledJob;
import com.godaddy.vps4.orchestration.scheduler.Utils;
import com.godaddy.vps4.orchestration.vm.Vps4DeleteExtraScheduledZombieJobsForVm;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Vps4DeleteExtraScheduledZombieJobsForVmTest {
    Vps4DeleteExtraScheduledZombieJobsForVm command;
    ScheduledJobService scheduledJobService;

    private CommandContext context;
    private UUID vmId = UUID.randomUUID();
    private final List<ScheduledJob> scheduledJobs = new ArrayList<>();

    @Captor private ArgumentCaptor<DeleteScheduledJob.Request> deleteScheduledJobReqCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        scheduledJobService = mock(ScheduledJobService.class);
        command = new Vps4DeleteExtraScheduledZombieJobsForVm(scheduledJobService);
        setupScheduledJobs();
        setupMockContext();
    }

    private void setupScheduledJobs() {
        scheduledJobs.add(new ScheduledJob(UUID.randomUUID(), vmId, ScheduledJob.ScheduledJobType.ZOMBIE, Instant.now()));
        scheduledJobs.add(new ScheduledJob(UUID.randomUUID(), vmId, ScheduledJob.ScheduledJobType.ZOMBIE, Instant.now()));
    }

    private void setupMockContext() {
        context = mock(CommandContext.class);
        when(scheduledJobService.getScheduledJobsByType(vmId, ScheduledJob.ScheduledJobType.ZOMBIE))
                .thenReturn(scheduledJobs);
    }

    @Test
    public void callsDeleteJobOnlyOnExtraZombieScheduledJob() {
        command.execute(context, vmId);

        verify(scheduledJobService, times(1))
                .getScheduledJobsByType(vmId, ScheduledJob.ScheduledJobType.ZOMBIE);

        ScheduledJob job = scheduledJobs.get(0);
        verify(context, times(0))
                .execute(eq(String.format("DeleteScheduledJob-%s", job.id)), eq(DeleteScheduledJob.class), any());
        job = scheduledJobs.get(1);
        verify(context, times(1))
                .execute(eq(String.format("DeleteScheduledJob-%s", job.id)), eq(DeleteScheduledJob.class), deleteScheduledJobReqCaptor.capture());

        DeleteScheduledJob.Request req = deleteScheduledJobReqCaptor.getValue();
        Assert.assertEquals(job.id, req.jobId);
        Assert.assertEquals(Utils.getJobRequestClassForType(job.type), req.jobRequestClass);
    }
}
