package com.godaddy.vps4.intent;

import com.godaddy.vps4.intent.jdbc.JdbcIntentService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class IntentModule extends AbstractModule {
    @Override
    public void configure() {
        bind(IntentService.class).to(JdbcIntentService.class).in(Scopes.SINGLETON);
    }
}
