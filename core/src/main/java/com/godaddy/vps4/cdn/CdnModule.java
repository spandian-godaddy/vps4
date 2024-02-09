package com.godaddy.vps4.cdn;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class CdnModule extends AbstractModule {
    @Override
    public void configure() {
        install(new CdnClientServiceModule());

        bind(CdnService.class).to(DefaultCdnService.class).in(Scopes.SINGLETON);
    }
}
