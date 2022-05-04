package com.godaddy.vps4.oh;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class OhModule extends AbstractModule {
    @Override
    public void configure() {
        install(new OhClientModule());
        bind(OhBackupService.class).to(DefaultOhBackupService.class).in(Scopes.SINGLETON);
    }
}
