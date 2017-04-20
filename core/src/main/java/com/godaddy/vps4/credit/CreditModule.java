package com.godaddy.vps4.credit;

import com.godaddy.vps4.credit.jdbc.JdbcCreditService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import gdg.hfs.vhfs.ecomm.ECommService;

public class CreditModule extends AbstractModule{

    @Override
    public void configure() {
        bind(CreditService.class).to(JdbcCreditService.class).in(Singleton.class);
//        bind(CreditService.class).to(ECommCreditService.class).in(Singleton.class);
    }
}
