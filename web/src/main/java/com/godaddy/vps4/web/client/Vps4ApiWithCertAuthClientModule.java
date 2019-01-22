package com.godaddy.vps4.web.client;

import static com.godaddy.vps4.client.ClientUtils.getClientCertAuthServiceProvider;

import javax.inject.Singleton;

import com.godaddy.vps4.client.Vps4ClientModule;
import com.google.inject.AbstractModule;

public class Vps4ApiWithCertAuthClientModule extends AbstractModule {

    private final String clientCertKeyPath;
    private final String clientCertPath;

    public Vps4ApiWithCertAuthClientModule(String clientCertKeyPath, String clientCertPath) {
        this.clientCertKeyPath = clientCertKeyPath;
        this.clientCertPath = clientCertPath;
    }

    @Override
    public void configure() {
        String baseUrlConfigPropName = "vps4.api.url";

        install(new Vps4ClientModule());

        // VM Snapshot endpoint
        bind(VmSnapshotService.class)
            .toProvider(getClientCertAuthServiceProvider(VmSnapshotService.class, baseUrlConfigPropName, this.clientCertKeyPath, this.clientCertPath))
            .in(Singleton.class);

        // VM endpoint
        bind(VmService.class)
            .toProvider(getClientCertAuthServiceProvider(VmService.class, baseUrlConfigPropName, this.clientCertKeyPath, this.clientCertPath))
            .in(Singleton.class);

        // VM Support User endpoint
        bind(VmSupportUserService.class)
            .toProvider(getClientCertAuthServiceProvider(VmSupportUserService.class, baseUrlConfigPropName, this.clientCertKeyPath, this.clientCertPath))
            .in(Singleton.class);

        // VM Zombie endpoint
        bind(VmZombieService.class)
            .toProvider(getClientCertAuthServiceProvider(VmZombieService.class, baseUrlConfigPropName, this.clientCertKeyPath, this.clientCertPath))
            .in(Singleton.class);
    }
}
