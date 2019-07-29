package com.godaddy.vps4.panopta;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class PanoptaModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(PanoptaService.class).in(Scopes.SINGLETON);
    }
}
