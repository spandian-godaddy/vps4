package com.godaddy.vps4.panopta;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

public class PanoptaClientModule extends AbstractModule {
    @Override
    public void configure() {
        String baseUrl = "panopta.api.base.url";

        bind(PanoptaApiCustomerService.class)
                .toProvider(new PanoptaClientServiceProvider<>(baseUrl, PanoptaApiCustomerService.class))
                .in(Singleton.class);
        bind(PanoptaApiServerService.class)
                .toProvider(new PanoptaClientServiceProvider<>(baseUrl, PanoptaApiServerService.class))
                .in(Singleton.class);
        bind(PanoptaApiServerGroupService.class)
                .toProvider(new PanoptaClientServiceProvider<>(baseUrl, PanoptaApiServerGroupService.class))
                .in(Singleton.class);

        bind(PanoptaClientRequestFilter.class);
        bind(PanoptaClientResponseFilter.class);
    }
}
