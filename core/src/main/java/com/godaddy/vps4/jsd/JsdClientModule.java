package com.godaddy.vps4.jsd;

import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class JsdClientModule extends AbstractModule {
    @Override
    public void configure() {
        String baseUrlConfigPropName = "jsd.api.url";

        bind(JsdApiService.class)
                .toProvider(new JsdClientServiceProvider<>(baseUrlConfigPropName, JsdApiService.class))
                .in(Singleton.class);

        bind(JsdClientRequestFilter.class);
        bind(JsdClientResponseFilter.class);
    }
}
