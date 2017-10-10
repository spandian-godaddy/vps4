package com.godaddy.vps4.scheduler.core.quartz;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.quartz.impl.matchers.GroupMatcher.triggerGroupEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzSchedulerServiceActionsTest {

    private static final Logger logger = LoggerFactory.getLogger(QuartzSchedulerServiceActionsTest.class);
    static Injector injector;
    static Scheduler mockScheduler;

    @Inject private SchedulerService schedulerService;
    @Inject private Scheduler scheduler;
    @Inject private ObjectMapper objectMapper;
    String product;
    String jobGroup;

    @BeforeClass
    public static void newInjector() {
        mockScheduler = mock(Scheduler.class);
        injector = Guice.createInjector(
            new ObjectMapperModule(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Scheduler.class).toInstance(mockScheduler);
                    bind(SchedulerService.class).to(QuartzSchedulerService.class).in(Scopes.SINGLETON);
                }
            }
        );
    }

    @Before
    public void setUp() throws Exception {
        injector.injectMembers(this);
        product = "vps4";
        jobGroup = "backups";
    }

    @Test
    public void callingStartCallsStartOnTheScheduler() throws Exception {
        schedulerService.startScheduler();
        verify(mockScheduler, times(1)).start();
    }

    @Test
    public void callingStopCallsStopOnTheScheduler() throws Exception {
        schedulerService.stopScheduler();
        verify(mockScheduler, times(1)).shutdown();
    }

    @Test
    public void registerTriggerListenerForJobGroup() throws Exception {
        ListenerManager listenerManager = mock(ListenerManager.class);
        when(scheduler.getListenerManager()).thenReturn(listenerManager);

        String jobGroupId = "jobGroup1";
        SchedulerTriggerListener schedulerTriggerListener = new SchedulerTriggerListener() {
            @Override
            public String getName() {
                return "triggerListener";
            }
        };

        schedulerService.registerTriggerListenerForJobGroup(jobGroupId, schedulerTriggerListener);
        verify(listenerManager, times(1))
            .addTriggerListener(eq(schedulerTriggerListener), eq(triggerGroupEquals(jobGroupId)));
    }

    public static class TestJob extends SchedulerJob {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        }
    }
}