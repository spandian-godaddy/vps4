package com.godaddy.vps4.scheduler.plugin;

import com.godaddy.vps4.scheduler.plugin.backups.Vps4BackupJob;
import com.godaddy.vps4.scheduler.plugin.destroyVm.Vps4DestroyVmJob;
import com.godaddy.vps4.scheduler.plugin.cancelAccount.Vps4CancelAccountJob;
import com.godaddy.vps4.scheduler.plugin.patch.Vps4PatchJob;
import com.godaddy.vps4.scheduler.plugin.supportUser.Vps4RemoveSupportUserJob;
import com.godaddy.vps4.scheduler.plugin.supportUser.Vps4SupportUserJob;
import com.godaddy.vps4.scheduler.plugin.zombie.Vps4ZombieCleanupJob;
import com.google.inject.AbstractModule;

public class Vps4SchedulerJobModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Vps4BackupJob.class);
        bind(Vps4PatchJob.class);
        bind(Vps4SupportUserJob.class);
        bind(Vps4ZombieCleanupJob.class);
        bind(Vps4RemoveSupportUserJob.class);
        bind(Vps4DestroyVmJob.class);
        bind(Vps4CancelAccountJob.class);
    }
}
