package com.godaddy.vps4.scheduler.core.config;

import com.godaddy.hfs.config.Config;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ConfigModule extends AbstractModule {

    @Override
    public void configure() {
        bind(Config.class).toProvider(ConfigProvider.class).in(Scopes.SINGLETON);
    }
}
