package com.godaddy.vps4.scheduler.plugin;

import com.godaddy.vps4.scheduler.core.SchedulerPluginModule;
import com.godaddy.vps4.web.client.Vps4ApiWithCertAuthClientModule;
import com.godaddy.vps4.web.client.Vps4ApiWithSSOAuthClientModule;
import com.google.inject.AbstractModule;

public class Vps4SchedulerPluginModule extends AbstractModule implements SchedulerPluginModule {

    @Override
    protected void configure() {


        if (System.getProperty("scheduler.useJwtAuth", "false").equals("true")){
            install(new Vps4ApiWithSSOAuthClientModule());
        }
        else {
            install(new Vps4ApiWithCertAuthClientModule("scheduler.client.keyPath", "scheduler.client.certPath"));
        }

        install(new Vps4SchedulerJobModule());
        install(new Vps4SchedulerTriggerListenerModule());
    }
}
