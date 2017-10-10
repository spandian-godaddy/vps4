package com.godaddy.vps4.scheduler.core.quartz;

import com.godaddy.vps4.scheduler.core.SchedulerService;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobFactory;
import org.quartz.spi.JobStore;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(QuartzModule.class);

    @Override
    protected void configure() {
        bind(JobFactory.class).to(QuartzJobFactory.class).in(Scopes.SINGLETON);
        bind(SchedulerService.class).to(QuartzSchedulerService.class).in(Scopes.SINGLETON);
        bind(SchedulerFactory.class).toProvider(QuartzSchedulerFactoryProvider.class).in(Scopes.SINGLETON);
        bind(Scheduler.class).toProvider(QuartzSchedulerProvider.class).asEagerSingleton();
    }

    @Singleton
    @Provides
    public ThreadPool getThreadPool() {
        return new SimpleThreadPool(10, Thread.NORM_PRIORITY);
    }

    @Singleton
    @Provides
    @VanillaQuartz
    public SchedulerFactory getSimpleSchedulerFactory() {
        logger.info("******** simple scheduler factory was created *********");
        return new StdSchedulerFactory();
    }
}
