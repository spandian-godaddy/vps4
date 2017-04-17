package com.godaddy.vps4.credit;

import com.godaddy.vps4.credit.jdbc.JdbcVps4CreditService;
import com.google.inject.AbstractModule;

public class CreditModule extends AbstractModule{

    @Override
    public void configure() {
        bind(Vps4CreditService.class).to(JdbcVps4CreditService.class);
    }
}
