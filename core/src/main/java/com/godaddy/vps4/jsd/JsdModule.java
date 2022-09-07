package com.godaddy.vps4.jsd;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class JsdModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new JsdClientModule());
        bind(JsdService.class).to(DefaultJsdService.class).in(Scopes.SINGLETON);
    }
}
