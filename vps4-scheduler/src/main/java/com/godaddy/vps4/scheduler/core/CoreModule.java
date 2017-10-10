package com.godaddy.vps4.scheduler.core;

import com.godaddy.vps4.scheduler.core.quartz.QuartzModule;
import com.godaddy.vps4.scheduler.core.quartz.QuartzSchedulerService;
import com.godaddy.vps4.scheduler.web.scheduler.SchedulerResource;
import com.google.inject.AbstractModule;

public class CoreModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new QuartzModule());
    }
}
