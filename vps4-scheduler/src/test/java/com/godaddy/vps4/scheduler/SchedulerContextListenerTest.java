package com.godaddy.vps4.scheduler;

import static org.mockito.Mockito.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.godaddy.vps4.scheduler.core.utils.Utils;
import com.godaddy.vps4.scheduler.web.scheduler.jobs.TestJobOne;
import com.godaddy.vps4.scheduler.web.scheduler.jobs.TestJobThree;
import com.godaddy.vps4.scheduler.web.scheduler.jobs.TestJobTwo;
import com.godaddy.vps4.scheduler.web.scheduler.listeners.TriggerListenerOne;
import com.godaddy.vps4.scheduler.web.scheduler.listeners.TriggerListenerThree;
import com.godaddy.vps4.scheduler.web.scheduler.listeners.TriggerListenerTwo;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContextEvent;

import static org.junit.Assert.*;

public class SchedulerContextListenerTest {

    Injector injector;
    SchedulerService mockSchedulerService;
    ServletContextEvent mockServletContextEvent;

    @Inject private SchedulerContextListener schedulerContextListener;

    @Before
    public void setUp() throws Exception {
        mockSchedulerService = mock(SchedulerService.class);
        injector = Guice.createInjector(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(SchedulerService.class).toInstance(mockSchedulerService);
                        bind(TestJobOne.class);
                        bind(TestJobTwo.class);
                        bind(TestJobThree.class);

                        bind(TriggerListenerOne.class);
                        bind(TriggerListenerTwo.class);
                        bind(TriggerListenerThree.class);
                    }
                }
        );
        injector.injectMembers(this);
        mockServletContextEvent = mock(ServletContextEvent.class);
    }

    @Test
    public void registersJobClassesOnceServletContextIsInitialized() throws Exception {
        schedulerContextListener.contextInitialized(mockServletContextEvent);

        verify(mockSchedulerService, times(1))
            .registerJobClassForJobGroup(
                eq(Utils.getJobGroupId("product1", "group1")),
                eq(TestJobOne.class));
        verify(mockSchedulerService, times(1))
                .registerJobClassForJobGroup(
                        eq(Utils.getJobGroupId("product1", "group2")),
                        eq(TestJobTwo.class));
        verify(mockSchedulerService, times(1))
                .registerJobClassForJobGroup(
                        eq(Utils.getJobGroupId("product2", "group1")),
                        eq(TestJobThree.class));
    }

    @Test
    public void registersTriggerListenersOnceServletContextIsInitialized() throws Exception {
        schedulerContextListener.contextInitialized(mockServletContextEvent);

        verify(mockSchedulerService, times(1))
                .registerTriggerListenerForJobGroup(
                        eq(Utils.getJobGroupId("product1", "group1")),
                        any(TriggerListenerOne.class));
        verify(mockSchedulerService, times(1))
                .registerTriggerListenerForJobGroup(
                        eq(Utils.getJobGroupId("product1", "group2")),
                        any(TriggerListenerTwo.class));
        verify(mockSchedulerService, times(1))
                .registerTriggerListenerForJobGroup(
                        eq(Utils.getJobGroupId("product2", "group1")),
                        any(TriggerListenerThree.class));
    }

    @Test
    public void startsSchedulerOnceServletContextIsInitialized() throws Exception {
        schedulerContextListener.contextInitialized(mockServletContextEvent);
        verify(mockSchedulerService, times(1)).startScheduler();
    }

    @Test
    public void stopsSchedulerOnceServletContextIsDestroyed() throws Exception {
        schedulerContextListener.contextDestroyed(mockServletContextEvent);
        verify(mockSchedulerService, times(1)).stopScheduler();
    }
}