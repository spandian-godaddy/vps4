package com.godaddy.vps4.panopta;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

public class PanoptaClientModule extends AbstractModule {
    @Override
    public void configure() {
        String baseUrlConfigPropName = "panopta.api.base.url";

        bind(PanoptaApiCustomerService.class)
                .toProvider(new PanoptaClientServiceProvider<>(baseUrlConfigPropName, PanoptaApiCustomerService.class))
                .in(Singleton.class);
        bind(PanoptaApiServerService.class)
                .toProvider(new PanoptaClientServiceProvider<>(baseUrlConfigPropName, PanoptaApiServerService.class))
                .in(Singleton.class);
        bind(PanoptaApiServerGroupService.class)
                .toProvider(new PanoptaClientServiceProvider<>(baseUrlConfigPropName, PanoptaApiServerGroupService.class))
                .in(Singleton.class);
        bind(PanoptaApiOutageService.class)
                .toProvider(new PanoptaClientServiceProvider<>(baseUrlConfigPropName, PanoptaApiOutageService.class))
                .in(Singleton.class);

        bind(PanoptaClientRequestFilter.class);
        bind(PanoptaClientResponseFilter.class);
    }
}
