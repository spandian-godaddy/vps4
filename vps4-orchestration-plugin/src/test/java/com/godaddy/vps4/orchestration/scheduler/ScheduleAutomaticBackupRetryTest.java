package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.scheduler.core.JobType;
import com.godaddy.vps4.scheduler.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.plugin.backups.Vps4BackupJob;
import com.godaddy.vps4.scheduler.web.client.SchedulerService;
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

import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Mockito.*;

public class ScheduleAutomaticBackupRetryTest {
    static Injector injector;

    private CommandContext context;
    private ScheduleAutomaticBackupRetry.Request request;
    private UUID jobId = UUID.randomUUID();
    private SchedulerJobDetail jobDetail;

    @Inject ScheduleAutomaticBackupRetry command;
    @Inject @ClientCertAuth SchedulerService schedulerService;

    @Captor private ArgumentCaptor<Function<CommandContext, SchedulerJobDetail>> createJobCaptor;
    @Captor private ArgumentCaptor<Vps4BackupJob.Request> schedulerJobCreationDataCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
            new ConfigModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    SchedulerService mockSchedulerService = mock(SchedulerService.class);
                    bind(SchedulerService.class)
                        .annotatedWith(ClientCertAuth.class)
                        .toInstance(mockSchedulerService);
                }
            }
        );
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        MockitoAnnotations.initMocks(this);
        setupMockContext();
        setCommandRequest();
    }

    private void setupMockContext() {
        context = mock(CommandContext.class);
    }

    private void setCommandRequest() {
        request = new ScheduleAutomaticBackupRetry.Request();
    }

    @Test
    public void callsSchedulerServiceToCreateJobSchedule() throws Exception {
        jobDetail = new SchedulerJobDetail(jobId, null, null);
        when(context.execute(eq("Retry Scheduled Backup"), any(Function.class), eq(SchedulerJobDetail.class)))
                .thenReturn(jobDetail);

        command.execute(context, request);

        // verify call to the scheduler service is wrapped in a context.execute method
        verify(context, times(1))
            .execute(eq("Retry Scheduled Backup"), createJobCaptor.capture(), eq(SchedulerJobDetail.class));

        // Verify that the lambda is calling the appropriate scheduler service method
        Function<CommandContext, SchedulerJobDetail> lambda = createJobCaptor.getValue();
        SchedulerJobDetail ret = lambda.apply(context);
        verify(schedulerService, times(1))
            .submitJobToGroup(eq("vps4"), eq("backups"), schedulerJobCreationDataCaptor.capture());

        // verify the job creation payload data
        Vps4BackupJob.Request jobRequestData = schedulerJobCreationDataCaptor.getValue();
        Assert.assertEquals(request.vmId, jobRequestData.vmId);
        Assert.assertEquals(JobType.ONE_TIME, jobRequestData.jobType);
        Assert.assertEquals(null, jobRequestData.repeatIntervalInDays);
    }

    @Test(expected = RuntimeException.class)
    public void errorInSchedulerJobCreation() {
        when(context.execute(eq("Retry Scheduled Backup"), any(Function.class), eq(SchedulerJobDetail.class)))
                .thenThrow(new RuntimeException("Unit Test Error"));
        command.execute(context, request);
    }

}