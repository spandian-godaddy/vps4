package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.plugin.Vps4ZombieCleanupJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScheduleZombieVmCleanupTest {
    static Injector injector;

    private CommandContext context;
    private final UUID vmId = UUID.randomUUID();
    private UUID jobId = UUID.randomUUID();
    private SchedulerJobDetail jobDetail;

    @Inject ScheduleZombieVmCleanup command;
    @Inject @ClientCertAuth SchedulerWebService schedulerWebService;

    @Captor private ArgumentCaptor<Function<CommandContext, SchedulerJobDetail>> createJobCaptor;
    @Captor private ArgumentCaptor<Vps4ZombieCleanupJobRequest> schedulerJobCreationDataCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new ConfigModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        SchedulerWebService mockSchedulerWebService = mock(SchedulerWebService.class);
                        bind(SchedulerWebService.class)
                                .annotatedWith(ClientCertAuth.class)
                                .toInstance(mockSchedulerWebService);
                    }
                }
        );
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
        setupMockContext();
    }

    private void setupMockContext() {
        context = mock(CommandContext.class);
    }

    @Test
    public void callsSchedulerServiceToCreateJobSchedule() throws Exception {
        jobDetail = new SchedulerJobDetail(jobId, null, null);
        when(context.execute(eq("Cleanup Zombie VM"), any(Function.class), eq(SchedulerJobDetail.class)))
                .thenReturn(jobDetail);

        Instant now = Instant.now();
        command.execute(context, vmId);

        // verify call to the scheduler service is wrapped in a context.execute method
        verify(context, times(1))
                .execute(eq("Cleanup Zombie VM"), createJobCaptor.capture(), eq(SchedulerJobDetail.class));

        // Verify that the lambda is calling the appropriate scheduler service method
        Function<CommandContext, SchedulerJobDetail> lambda = createJobCaptor.getValue();
        SchedulerJobDetail ret = lambda.apply(context);
        verify(schedulerWebService, times(1))
                .submitJobToGroup(eq("vps4"), eq("zombie"), schedulerJobCreationDataCaptor.capture());

        // verify the job creation payload data
        Vps4ZombieCleanupJobRequest jobRequestData = schedulerJobCreationDataCaptor.getValue();
        Assert.assertEquals(vmId, jobRequestData.vmId);
        Assert.assertEquals(JobType.ONE_TIME, jobRequestData.jobType);
        // Slightly round about way of verifying that the cleanup job is scheduled to be 7 days out
        Assert.assertEquals(7, ChronoUnit.DAYS.between(now, jobRequestData.when));
    }

    @Test(expected = RuntimeException.class)
    public void errorInSchedulerJobCreation() {
        when(context.execute(eq("Cleanup Zombie VM"), any(Function.class), eq(SchedulerJobDetail.class)))
                .thenThrow(new RuntimeException("Error"));
        command.execute(context, vmId);
    }
}