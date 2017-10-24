package com.godaddy.vps4.web.client;

import com.godaddy.vps4.client.ClientCertAuth;
import com.godaddy.vps4.client.ClientCertAuthenticatedServiceProvider;
import com.godaddy.vps4.client.SsoJwtAuth;
import com.godaddy.vps4.client.SsoJwtAuthenticatedServiceProvider;
import com.godaddy.vps4.client.SsoTokenServiceModule;
import com.godaddy.vps4.client.Vps4ClientModule;
import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class VmSnapshotServiceClientModule extends AbstractModule {

    @Override
    public void configure() {
        install(new Vps4ClientModule());

        bind(VmSnapshotService.class)
            .annotatedWith(SsoJwtAuth.class)
            .toProvider(getSsoAuthVmSnapshotServiceProvider())
            .in(Singleton.class);

        bind(VmSnapshotService.class)
            .annotatedWith(ClientCertAuth.class)
            .toProvider(getClientCertAuthVmSnapshotServiceProvider())
            .in(Singleton.class);
    }

    private SsoJwtAuthenticatedServiceProvider<VmSnapshotService> getSsoAuthVmSnapshotServiceProvider() {
        return new SsoJwtAuthenticatedServiceProvider<>("vps4.api.url", VmSnapshotService.class);
    }

    private ClientCertAuthenticatedServiceProvider<VmSnapshotService> getClientCertAuthVmSnapshotServiceProvider() {
        return new ClientCertAuthenticatedServiceProvider<>(
                "vps4.api.url",
                VmSnapshotService.class,
                "hfs.client.keyPath", // TODO: change this to the scheduler client key path
                "hfs.client.certPath"); // TODO: change this to the scheduler client cert path
    }
}
