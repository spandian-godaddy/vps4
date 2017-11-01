package com.godaddy.vps4.util;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class UtilsModule extends AbstractModule {

    @Override
    public void configure() {
        bind(Cryptography.class).toProvider(CryptographyProvider.class).in(Scopes.SINGLETON);
        bind(Monitoring.class).toProvider(MonitoringProvider.class).in(Scopes.SINGLETON);
    }

}
