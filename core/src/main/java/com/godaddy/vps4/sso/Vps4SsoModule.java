package com.godaddy.vps4.sso;

import static com.godaddy.vps4.client.ClientUtils.getClientCertAuthServiceProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

public class Vps4SsoModule extends AbstractModule {
    @Override
    protected void configure() {
        MapBinder<CertJwtApi, Vps4SsoService> vps4SsoServiceBinder = MapBinder
                .newMapBinder(binder(), CertJwtApi.class, Vps4SsoService.class);

        vps4SsoServiceBinder.addBinding(CertJwtApi.HFS)
                            .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                                                                         "hfs.sso.url",
                                                                         "hfs.api.keyPath",
                                                                         "hfs.api.certPath"))
                            .in(Singleton.class);

        vps4SsoServiceBinder.addBinding(CertJwtApi.MESSAGING)
                            .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                                                                         "sso.url",
                                                                         "messaging.api.keyPath",
                                                                         "messaging.api.certPath"))
                            .in(Singleton.class);

    }
}
