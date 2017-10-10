package com.godaddy.vps4.scheduler.core.quartz;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.spi.JobFactory;

public class QuartzSchedulerProvider implements Provider<Scheduler> {
    private final Scheduler scheduler;

    @Inject
    public QuartzSchedulerProvider(SchedulerFactory schedulerFactory, JobFactory jobFactory)
            throws SchedulerException
    {
        scheduler = schedulerFactory.getScheduler();
        scheduler.setJobFactory(jobFactory);
    }

    @Override
    public Scheduler get() {
        return scheduler;
    }
}
