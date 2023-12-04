package com.godaddy.vps4.firewall;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class FirewallClientServiceModule extends AbstractModule {
    @Override
    public void configure() {
        String baseUrl = "firewall.api.base.url";

        bind(FirewallClientService.class)
                .toProvider(new FirewallClientServiceProvider<>(baseUrl, FirewallClientService.class))
                .in(Singleton.class);

        bind(FirewallClientResponseFilter.class);
    }
}
