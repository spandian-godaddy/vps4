package com.godaddy.vps4.web.client;

import com.godaddy.vps4.client.SsoJwtAuthenticatedServiceProvider;
import com.godaddy.vps4.client.Vps4ClientModule;
import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class SsoVmSnapshotServiceClientModule extends AbstractModule {

    @Override
    public void configure() {
        install(new Vps4ClientModule());

        bind(VmSnapshotService.class)
            .toProvider(getSsoAuthVmSnapshotServiceProvider())
            .in(Singleton.class);


    }

    private SsoJwtAuthenticatedServiceProvider<VmSnapshotService> getSsoAuthVmSnapshotServiceProvider() {
        return new SsoJwtAuthenticatedServiceProvider<>("vps4.api.url", VmSnapshotService.class);
    }
}
