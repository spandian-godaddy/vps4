package com.godaddy.vps4.reseller;

import com.godaddy.vps4.reseller.jdbc.JdbcResellerService;
import com.google.inject.AbstractModule;

public class ResellerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ResellerService.class).to(JdbcResellerService.class);
    }
}
