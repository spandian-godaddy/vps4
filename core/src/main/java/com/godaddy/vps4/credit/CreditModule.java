package com.godaddy.vps4.credit;

import javax.sql.DataSource;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.credit.jdbc.JdbcCreditService;
import com.godaddy.vps4.vm.DataCenterService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import gdg.hfs.vhfs.ecomm.ECommService;

public class CreditModule extends AbstractModule{

    @Override
    public void configure() {
    }

    @Provides @Singleton
    CreditService provideCreditService(Config config, ECommService ecommService,
            DataCenterService dataCenterService, DataSource dataSource) {
        if (config.get("credit.service").equals("cassandra"))
            return new ECommCreditService(ecommService, dataCenterService);
        else
            return new JdbcCreditService(dataSource);
    }
}
