package com.godaddy.vps4.orchestration.scheduler;

import com.google.inject.AbstractModule;

public class SchedulerModule extends AbstractModule {

    @Override
    public void configure() {
        bind(SetupAutomaticBackupSchedule.class);
        bind(ScheduleAutomaticBackupRetry.class);
        bind(DeleteAutomaticBackupSchedule.class);
        bind(ScheduleZombieVmCleanup.class);
        bind(DeleteScheduledJob.class);
    }
}
