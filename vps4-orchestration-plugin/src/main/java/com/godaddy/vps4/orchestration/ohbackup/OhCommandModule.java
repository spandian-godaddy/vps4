package com.godaddy.vps4.orchestration.ohbackup;

import com.google.inject.AbstractModule;

public class OhCommandModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(WaitForOhJob.class);
        bind(Vps4CreateOhBackup.class);
        bind(Vps4DestroyOhBackup.class);
        bind(Vps4RestoreOhBackup.class);
    }
}
