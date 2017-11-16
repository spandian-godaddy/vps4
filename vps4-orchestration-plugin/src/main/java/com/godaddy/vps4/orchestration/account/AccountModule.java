package com.godaddy.vps4.orchestration.account;

import com.google.inject.AbstractModule;

public class AccountModule extends AbstractModule {

    @Override
    public void configure() {
        bind(Vps4ProcessAccountCancellation.class);
    }
}
