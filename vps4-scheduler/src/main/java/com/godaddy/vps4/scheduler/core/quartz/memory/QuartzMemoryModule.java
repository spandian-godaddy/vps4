package com.godaddy.vps4.scheduler.core.quartz.memory;

import com.google.inject.AbstractModule;
import org.quartz.simpl.RAMJobStore;
import org.quartz.spi.JobStore;

public class QuartzMemoryModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(JobStore.class).toInstance(new RAMJobStore());
    }
}
