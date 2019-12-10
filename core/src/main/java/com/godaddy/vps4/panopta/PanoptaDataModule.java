package com.godaddy.vps4.panopta;

import com.godaddy.vps4.panopta.jdbc.JdbcPanoptaDataService;
import com.google.inject.AbstractModule;

public class PanoptaDataModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PanoptaDataService.class).to(JdbcPanoptaDataService.class);
    }

}
