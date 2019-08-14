package com.godaddy.vps4.panopta;

import com.godaddy.vps4.panopta.jdbc.JdbcPanoptaDataService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class PanoptaModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new PanoptaClientModule());
        bind(PanoptaService.class).to(DefaultPanoptaService.class).in(Scopes.SINGLETON);
        bind(PanoptaDataService.class).to(JdbcPanoptaDataService.class);
    }
}
