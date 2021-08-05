package com.godaddy.vps4.orchestration.scheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.orchestration.vm.Vps4RecordScheduledJobForVm;
import com.godaddy.vps4.scheduler.api.core.JobType;
import com.godaddy.vps4.scheduler.api.core.SchedulerJobDetail;
import com.godaddy.vps4.scheduler.api.plugin.Vps4CancelAccountJobRequest;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandContext;

public class ScheduleCancelAccountTest {
    static Injector injector;

    private CommandContext context;
    private final UUID vmId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();
    private SchedulerJobDetail jobDetail;

    @Inject ScheduleCancelAccount command;
    @Inject SchedulerWebService schedulerWebService;

    @Captor private ArgumentCaptor<Function<CommandContext, SchedulerJobDetail>> createJobCaptor;
    @Captor private ArgumentCaptor<Vps4CancelAccountJobRequest> schedulerJobCreationDataCaptor;
    @Captor private ArgumentCaptor<Vps4RecordScheduledJobForVm.Request> recordJobArgumentCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
                new ConfigModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        SchedulerWebService mockSchedulerWebService = mock(SchedulerWebService.class);
                        bind(SchedulerWebService.class).toInstance(mockSchedulerWebService);
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
        jobDetail = new SchedulerJobDetail(jobId, null, null, false);
        when(context.execute(eq("Retry Cancel Account"), any(Function.class), eq(SchedulerJobDetail.class)))
                .thenReturn(jobDetail);

        command.execute(context, vmId);

        // verify call to the scheduler service is wrapped in a context.execute method
        verify(context, times(1))
                .execute(eq("Retry Cancel Account"), createJobCaptor.capture(), eq(SchedulerJobDetail.class));

        // Verify that the lambda is calling the appropriate scheduler service method
        Function<CommandContext, SchedulerJobDetail> lambda = createJobCaptor.getValue();
        lambda.apply(context);
        verify(schedulerWebService, times(1))
                .submitJobToGroup(eq("vps4"), eq("cancelAccount"), schedulerJobCreationDataCaptor.capture());

        // verify the job creation payload data
        Vps4CancelAccountJobRequest jobRequestData = schedulerJobCreationDataCaptor.getValue();
        Assert.assertEquals(vmId, jobRequestData.vmId);
        Assert.assertEquals(JobType.ONE_TIME, jobRequestData.jobType);
    }

    @Test(expected = RuntimeException.class)
    public void errorInSchedulerJobCreation() {
        when(context.execute(eq("Retry Cancel Account"), any(Function.class), eq(SchedulerJobDetail.class)))
                .thenThrow(new RuntimeException("Error"));
        command.execute(context, vmId);
    }

    @Test
    public void callsRecordJobId() {
        CommandContext mockContext = mock(CommandContext.class);
        when(mockContext.getId()).thenReturn(UUID.randomUUID());

        jobDetail = new SchedulerJobDetail(jobId, null, null, false);
        when(context.execute(eq("Retry Cancel Account"), any(Function.class), eq(SchedulerJobDetail.class))).thenReturn(jobDetail);

        command.execute(context, vmId);

        verify(context, times(1)).execute(eq("RecordScheduledJobId"), eq(Vps4RecordScheduledJobForVm.class), recordJobArgumentCaptor.capture());
    }
}
