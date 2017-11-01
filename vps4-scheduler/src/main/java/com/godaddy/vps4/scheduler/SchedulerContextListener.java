package com.godaddy.vps4.scheduler;

import com.godaddy.vps4.scheduler.core.JobMetadata;
import com.godaddy.vps4.scheduler.core.SchedulerJob;
import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.godaddy.vps4.scheduler.core.SchedulerTriggerListener;
import com.godaddy.vps4.scheduler.core.TriggerListenerMetadata;
import com.godaddy.vps4.scheduler.core.utils.Utils;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SchedulerContextListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(SchedulerContextListener.class);

    private final SchedulerService schedulerService;
    private final Injector injector;

    @Inject
    public SchedulerContextListener(Injector injector, SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
        this.injector = injector;
    }

    private void registerSchedulerPluginJobClasses() {
        @SuppressWarnings("unchecked")
        List<Class<SchedulerJob>> jobClasses
            = getPluginClassesForSchedulerClassType(SchedulerJob.class)
            .stream()
            .map(o -> (Class<SchedulerJob>)o)
            .collect(Collectors.toList());
        for(Class<SchedulerJob> jobClass: jobClasses) {
            registerJobClassWithSchedulerService(jobClass);
        }
    }

    private List<Class<?>> getPluginClassesForSchedulerClassType(Class<?> schedulerClassType) {
        Map<Key<?>,Binding<?>> mapOfBindings = injector.getBindings();
        return mapOfBindings
            .entrySet()
            .stream()
            .map(entry -> entry.getKey().getTypeLiteral().getRawType())
            .filter(cls -> schedulerClassType.isAssignableFrom(cls)
                    && (cls.isAnnotationPresent(JobMetadata.class)
                    || cls.isAnnotationPresent(TriggerListenerMetadata.class)))
            .collect(Collectors.toList());
    }

    private void registerJobClassWithSchedulerService(Class<? extends SchedulerJob> jobClass) {
        String product = Utils.getProductForJobClass(jobClass);
        String jobGroup = Utils.getJobGroupForJobClass(jobClass);
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        logger.info("******* Job Group: {} --> Job Class: {} ***********", jobGroupId, jobClass);
        schedulerService.registerJobClassForJobGroup(jobGroupId, jobClass);
    }

    private void registerSchedulerTriggerListeners() {
        @SuppressWarnings("unchecked")
        List<Class<SchedulerTriggerListener>> triggerClasses
            = getPluginClassesForSchedulerClassType(SchedulerTriggerListener.class)
            .stream()
            .map(o -> (Class<SchedulerTriggerListener>)o)
            .collect(Collectors.toList());

        for(Class<SchedulerTriggerListener> triggerClass: triggerClasses) {
            registerTriggerClassWithSchedulerService(triggerClass);
        }
    }

    private void registerTriggerClassWithSchedulerService(Class<SchedulerTriggerListener> triggerClass) {
        String product = Utils.getProductForTriggerListenerClass(triggerClass);
        String jobGroup = Utils.getJobGroupForTriggerListenerClass(triggerClass);
        String jobGroupId = Utils.getJobGroupId(product, jobGroup);
        logger.info("******* Job Group: {} --> Trigger Class: {} ***********", jobGroupId, triggerClass);
        try {
            schedulerService.registerTriggerListenerForJobGroup(jobGroupId, injector.getInstance(triggerClass));
        }
        catch (Exception e) {
            logger.error("error while registering trigger listener for job group with ID: {}", jobGroupId);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            logger.info("******** Starting up the scheduler service **************");
            registerSchedulerPluginJobClasses();
            registerSchedulerTriggerListeners();
            schedulerService.startScheduler();
        } catch (Exception ex) {
            logger.error("Error starting scheduler service", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            logger.info("******** Stopping the scheduler service **************");
            schedulerService.stopScheduler();
        } catch (Exception ex) {
            logger.error("Error stopping scheduler service", ex);
            throw new RuntimeException(ex);
        }
    }
}
