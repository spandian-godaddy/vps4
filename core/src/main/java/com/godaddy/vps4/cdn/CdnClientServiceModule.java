package com.godaddy.vps4.cdn;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class CdnClientServiceModule extends AbstractModule {
    @Override
    public void configure() {
        String baseUrl = "firewall.api.base.url";

        bind(CdnClientService.class)
                .toProvider(new CdnClientServiceProvider<>(baseUrl, CdnClientService.class))
                .in(Singleton.class);

        bind(CdnClientResponseFilter.class);
    }
}
