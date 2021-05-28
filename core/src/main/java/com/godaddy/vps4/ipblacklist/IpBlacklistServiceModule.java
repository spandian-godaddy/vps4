package com.godaddy.vps4.ipblacklist;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class IpBlacklistServiceModule extends AbstractModule {
    @Override
    public void configure() {
        String baseUrl = "ipblacklist.api.base.url";

        bind(IpBlacklistClientService.class)
                .toProvider(new IpBlacklistServiceProvider<>(baseUrl, IpBlacklistClientService.class))
                .in(Singleton.class);

        bind(IpBlacklistRequestFilter.class);
    }
}
