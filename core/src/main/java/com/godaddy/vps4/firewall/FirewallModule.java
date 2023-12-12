package com.godaddy.vps4.firewall;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class FirewallModule extends AbstractModule {
    @Override
    public void configure() {
        install(new FirewallClientServiceModule());

        bind(FirewallService.class).to(DefaultFirewallService.class).in(Scopes.SINGLETON);
    }
}
