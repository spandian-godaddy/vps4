package com.godaddy.vps4.oh;

import com.godaddy.vps4.oh.backups.DefaultOhBackupService;
import com.godaddy.vps4.oh.backups.OhBackupService;
import com.godaddy.vps4.oh.jobs.DefaultOhJobService;
import com.godaddy.vps4.oh.jobs.OhJobService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class OhModule extends AbstractModule {
    @Override
    public void configure() {
        install(new OhClientModule());
        bind(OhBackupService.class).to(DefaultOhBackupService.class).in(Scopes.SINGLETON);
        bind(OhJobService.class).to(DefaultOhJobService.class).in(Scopes.SINGLETON);
    }
}
