package com.godaddy.vps4.client;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class SsoTokenServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(SsoTokenService.class).to(SsoTokenServiceImpl.class).in(Scopes.SINGLETON);
    }
}
