package com.godaddy.vps4.scheduler.plugin;

import com.godaddy.vps4.scheduler.core.SchedulerPluginModule;
import com.godaddy.vps4.web.client.CertVmSnapshotServiceClientModule;
import com.godaddy.vps4.web.client.SsoVmSnapshotServiceClientModule;
import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class Vps4SchedulerPluginModule extends AbstractModule implements SchedulerPluginModule {

    @Override
    protected void configure() {


        if (System.getProperty("scheduler.useJwtAuth", "false").equals("true")){
            install(new SsoVmSnapshotServiceClientModule());
        }
        else {
            install(new CertVmSnapshotServiceClientModule());
        }

        install(new Vps4SchedulerJobModule());
        install(new Vps4SchedulerTriggerListenerModule());
    }
}
