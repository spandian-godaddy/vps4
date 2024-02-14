package com.godaddy.vps4.credit;

import com.google.inject.AbstractModule;

public class CreditModule extends AbstractModule{

    @Override
    public void configure() {
        bind(CreditService.class).to(ECommCreditService.class);
    }
}
