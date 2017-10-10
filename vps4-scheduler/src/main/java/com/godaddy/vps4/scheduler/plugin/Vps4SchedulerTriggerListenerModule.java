package com.godaddy.vps4.scheduler.plugin;

import com.godaddy.vps4.scheduler.plugin.backups.Vps4BackupTriggerListener;
import com.godaddy.vps4.scheduler.plugin.patch.Vps4PatchTriggerListener;
import com.godaddy.vps4.scheduler.plugin.supportUser.Vps4SupportUserTriggerListener;
import com.google.inject.AbstractModule;

public class Vps4SchedulerTriggerListenerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Vps4BackupTriggerListener.class);
        bind(Vps4PatchTriggerListener.class);
        bind(Vps4SupportUserTriggerListener.class);
    }
}
