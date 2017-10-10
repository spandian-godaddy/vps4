package com.godaddy.vps4.scheduler.core.quartz;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzSchedulerFactoryProvider implements Provider<SchedulerFactory> {

    private static final Logger logger = LoggerFactory.getLogger(QuartzSchedulerFactoryProvider.class);
    private DirectSchedulerFactory factory;

    @Inject
    public QuartzSchedulerFactoryProvider(JobStore jobStore, ThreadPool threadPool) {
        factory = DirectSchedulerFactory.getInstance();
        try {
            factory.createScheduler(threadPool, jobStore);
            logger.info("******** configured scheduler factory was created *********");
        }
        catch (SchedulerException e) {
            logger.error("Error creating a configured scheduler factory", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public SchedulerFactory get() {
        return factory;
    }
}
