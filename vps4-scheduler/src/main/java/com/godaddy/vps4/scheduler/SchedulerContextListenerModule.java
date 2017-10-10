package com.godaddy.vps4.scheduler;

import com.google.inject.AbstractModule;

public class SchedulerContextListenerModule extends AbstractModule {

    @Override
    public void configure() {
        bind(SchedulerContextListener.class);
    }
}
