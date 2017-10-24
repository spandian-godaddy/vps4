package com.godaddy.vps4.client;

import com.google.inject.AbstractModule;

public class Vps4ClientModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new SsoTokenServiceModule());
        bind(String.class).annotatedWith(ShopperId.class).toProvider(ShopperIdProvider.class);
    }
}
