package com.godaddy.vps4.web.client;

import com.godaddy.vps4.client.Vps4ClientModule;
import com.godaddy.vps4.client.ClientCertAuthenticatedServiceProvider;
import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class CertVmSnapshotServiceClientModule extends AbstractModule {

    @Override
    public void configure() {
        install(new Vps4ClientModule());

        bind(VmSnapshotService.class)
            .toProvider(getClientCertAuthVmSnapshotServiceProvider())
            .in(Singleton.class);
    }

    private ClientCertAuthenticatedServiceProvider<VmSnapshotService> getClientCertAuthVmSnapshotServiceProvider() {
        return new ClientCertAuthenticatedServiceProvider<>(
                "vps4.api.url",
                VmSnapshotService.class,
                "scheduler.client.keyPath",
                "scheduler.client.certPath");
    }
}
