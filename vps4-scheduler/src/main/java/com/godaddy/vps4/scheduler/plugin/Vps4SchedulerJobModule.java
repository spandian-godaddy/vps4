package com.godaddy.vps4.scheduler.plugin;

import com.godaddy.vps4.scheduler.plugin.backups.Vps4BackupJob;
import com.godaddy.vps4.scheduler.plugin.patch.Vps4PatchJob;
import com.godaddy.vps4.scheduler.plugin.supportUser.Vps4SupportUserJob;
import com.google.inject.AbstractModule;

public class Vps4SchedulerJobModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Vps4BackupJob.class);
        bind(Vps4PatchJob.class);
        bind(Vps4SupportUserJob.class);
    }
}
