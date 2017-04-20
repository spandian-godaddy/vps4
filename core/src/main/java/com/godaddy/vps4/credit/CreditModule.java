package com.godaddy.vps4.credit;

import com.godaddy.vps4.credit.jdbc.JdbcCreditService;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class CreditModule extends AbstractModule{

    @Override
    public void configure() {

        bind(CreditService.class).to(JdbcCreditService.class).in(Singleton.class);
        // TODO: Disable createCreditIfNoneExists() in authenticate before swapping binding - Jira VPS4-469
        // bind(CreditService.class).to(ECommCreditService.class).in(Singleton.class);
    }
}
