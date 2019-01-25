package com.godaddy.vps4.orchestration.vm;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.orchestration.scheduler.DeleteScheduledJob;
import com.godaddy.vps4.orchestration.scheduler.Utils;
import com.godaddy.vps4.scheduledJob.ScheduledJob;
import com.godaddy.vps4.scheduledJob.ScheduledJobService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;

public class Vps4DeleteAllScheduledJobsForVmTest {
    static Injector injector;

    @Inject Vps4DeleteAllScheduledJobsForVm command;
    @Inject private ScheduledJobService scheduledJobService;

    private CommandContext context;
    private UUID vmId = UUID.randomUUID();
    private final List<ScheduledJob> scheduledJobs = new ArrayList<>();

    @Captor private ArgumentCaptor<Function<CommandContext, List>> getScheduledJobsCaptor;
    @Captor private ArgumentCaptor<DeleteScheduledJob.Request> deleteScheduledJobReqCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    ScheduledJobService mockScheduledJobService = mock(ScheduledJobService.class);
                    bind(ScheduledJobService.class).toInstance(mockScheduledJobService);
                }
            }
        );
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);

        setupScheduledJobs();
        setupMockContext();
    }

    private void setupScheduledJobs() {
        scheduledJobs.add(new ScheduledJob(UUID.randomUUID(), vmId, ScheduledJob.ScheduledJobType.BACKUPS_RETRY, Instant.now()));
        scheduledJobs.add(new ScheduledJob(UUID.randomUUID(), vmId, ScheduledJob.ScheduledJobType.ZOMBIE, Instant.now()));
    }

    private void setupMockContext() {
        context = mock(CommandContext.class);
        when(scheduledJobService.getScheduledJobs(vmId)).thenReturn(scheduledJobs);
    }

    @Test
    public void callsScheduledJobServiceToGetAListOfVmJobs() throws Exception {
        command.execute(context, vmId);

        verify(scheduledJobService, times(1)).getScheduledJobs(eq(vmId));
    }

    @Test
    public void callsDeleteJobOnEveryScheduledJob() throws Exception {
        command.execute(context, vmId);

        for(ScheduledJob job : scheduledJobs) {
            verify(context, times(1))
                .execute(eq(String.format("DeleteScheduledJob-%s", job.id)), eq(DeleteScheduledJob.class), deleteScheduledJobReqCaptor.capture());

            DeleteScheduledJob.Request req = deleteScheduledJobReqCaptor.getValue();
            Assert.assertEquals(job.id, req.jobId);
            Assert.assertEquals(Utils.getJobRequestClassForType(job.type), req.jobRequestClass);
        }
    }
}
