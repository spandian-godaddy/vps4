package com.godaddy.vps4.web.client;

import static com.godaddy.vps4.client.ClientUtils.getSsoAuthServiceProvider;

import com.godaddy.vps4.client.Vps4ClientModule;
import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class Vps4ApiWithSSOAuthClientModule extends AbstractModule {

    @Override
    public void configure() {
        String baseUrlConfigPropName = "vps4.api.url";
        install(new Vps4ClientModule());

        // VM Snapshot endpoint
        bind(VmSnapshotService.class)
            .toProvider(getSsoAuthServiceProvider(VmSnapshotService.class, baseUrlConfigPropName))
            .in(Singleton.class);

        // VM endpoint
        bind(VmService.class)
            .toProvider(getSsoAuthServiceProvider(VmService.class, baseUrlConfigPropName))
            .in(Singleton.class);

    }
}
