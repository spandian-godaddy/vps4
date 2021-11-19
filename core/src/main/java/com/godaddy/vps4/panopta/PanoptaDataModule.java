package com.godaddy.vps4.panopta;

import com.godaddy.vps4.panopta.jdbc.JdbcPanoptaDataService;
import com.godaddy.vps4.panopta.jdbc.JdbcPanoptaMetricService;
import com.google.inject.AbstractModule;

public class PanoptaDataModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PanoptaDataService.class).to(JdbcPanoptaDataService.class);
        bind(PanoptaMetricService.class).to(JdbcPanoptaMetricService.class);
    }

}
