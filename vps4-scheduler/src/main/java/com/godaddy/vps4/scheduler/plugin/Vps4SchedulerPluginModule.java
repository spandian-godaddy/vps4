package com.godaddy.vps4.scheduler.plugin;

import com.godaddy.vps4.scheduler.core.SchedulerPluginModule;
import com.google.inject.AbstractModule;

public class Vps4SchedulerPluginModule extends AbstractModule implements SchedulerPluginModule {

    @Override
    protected void configure() {
        install(new Vps4SchedulerJobModule());
        install(new Vps4SchedulerTriggerListenerModule());
    }
}
