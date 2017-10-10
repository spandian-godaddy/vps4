package com.godaddy.vps4.scheduler.web;

import com.godaddy.vps4.scheduler.web.scheduler.SchedulerResource;
import com.google.inject.AbstractModule;

public class WebModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SchedulerResource.class);
        bind(SchedulerExceptionMapper.class);
    }
}
