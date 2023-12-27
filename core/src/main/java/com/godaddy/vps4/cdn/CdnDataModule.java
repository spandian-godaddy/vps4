package com.godaddy.vps4.cdn;

import com.godaddy.vps4.cdn.jdbc.JdbcCdnDataService;
import com.google.inject.AbstractModule;

public class CdnDataModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CdnDataService.class).to(JdbcCdnDataService.class);
    }
}