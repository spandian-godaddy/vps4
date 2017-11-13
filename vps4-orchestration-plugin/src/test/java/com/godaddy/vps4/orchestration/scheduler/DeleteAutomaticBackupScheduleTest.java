package com.godaddy.vps4.orchestration.scheduler;

import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import gdg.hfs.orchestration.CommandContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteAutomaticBackupScheduleTest {
    static Injector injector;

    private CommandContext context;
    private UUID backupJobId = UUID.randomUUID();

    @Inject DeleteAutomaticBackupSchedule command;
    @Inject
    SchedulerWebService schedulerWebService;

    @Captor private ArgumentCaptor<Function<CommandContext, Void>> deleteJobCaptor;

    @BeforeClass
    public static void newInjector() {
        injector = Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    SchedulerWebService mockSchedulerWebService = mock(SchedulerWebService.class);
                    bind(SchedulerWebService.class)
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
    public void callsSchedulerServiceToDeleteJobSchedule() throws Exception {
        when(context.execute(eq("Delete schedule"), any(Function.class), eq(Void.class)))
                .thenReturn(null);

        command.execute(context, backupJobId);

        // verify call to the scheduler service is wrapped in a context.execute method
        verify(context, times(1))
            .execute(eq("Delete schedule"), deleteJobCaptor.capture(), eq(Void.class));

        // Verify that the lambda is calling the appropriate scheduler service method
        Function<CommandContext, Void> lambda = deleteJobCaptor.getValue();
        lambda.apply(context);
        verify(schedulerWebService, times(1))
            .deleteJob(eq("vps4"), eq("backups"), eq(backupJobId));
    }

    @Test(expected = RuntimeException.class)
    public void errorInSchedulerJobDeletion() {
        when(context.execute(eq("Delete schedule"), any(Function.class), eq(Void.class)))
                .thenThrow(new Exception("Error"));
        command.execute(context, backupJobId);
    }

}