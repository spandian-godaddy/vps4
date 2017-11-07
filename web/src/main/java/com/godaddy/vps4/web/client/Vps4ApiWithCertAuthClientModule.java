package com.godaddy.vps4.web.client;

import static com.godaddy.vps4.client.ClientUtils.getClientCertAuthServiceProvider;

import com.godaddy.vps4.client.Vps4ClientModule;
import com.google.inject.AbstractModule;

import javax.inject.Singleton;

public class Vps4ApiWithCertAuthClientModule extends AbstractModule {

    @Override
    public void configure() {
        String baseUrlConfigPropName = "vps4.api.url";
        String clientCertKeyPath = "scheduler.client.keyPath";
        String clientCertPath = "scheduler.client.certPath";

        install(new Vps4ClientModule());

        // VM Snapshot endpoint
        bind(VmSnapshotService.class)
            .toProvider(getClientCertAuthServiceProvider(VmSnapshotService.class,
                    baseUrlConfigPropName,
                    clientCertKeyPath,
                    clientCertPath))
            .in(Singleton.class);

        // VM endpoint
        bind(VmService.class)
            .toProvider(getClientCertAuthServiceProvider(VmService.class,
                    baseUrlConfigPropName,
                    clientCertKeyPath,
                    clientCertPath))
            .in(Singleton.class);
    }
}
