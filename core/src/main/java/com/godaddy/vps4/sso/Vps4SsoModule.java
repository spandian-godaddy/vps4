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

        vps4SsoServiceBinder.addBinding(CertJwtApi.ENTITLEMENTS)
                .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                        "sso.url",
                        "entitlements.api.keyPath",
                        "entitlements.api.certPath"))
                .in(Singleton.class);
      
        vps4SsoServiceBinder.addBinding(CertJwtApi.CDN)
                .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                        "sso.url",
                        "firewall.api.keyPath",
                        "firewall.api.certPath"))
                .in(Singleton.class);
      
        vps4SsoServiceBinder.addBinding(CertJwtApi.SHOPPER)
                .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                        "sso.url",
                        "shopper.api.keyPath",
                        "shopper.api.certPath"))
                .in(Singleton.class);

        vps4SsoServiceBinder.addBinding(CertJwtApi.VPS4)
                .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                        "sso.url",
                        "vps4.crossdc.api.keyPath",
                        "vps4.crossdc.api.certPath"))
                .in(Singleton.class);

        bind(Vps4SsoService.class)
                .toProvider(getClientCertAuthServiceProvider(Vps4SsoService.class,
                "sso.url",
                "sso.api.keyPath",
                "sso.api.certPath"))
                .in(Singleton.class);
    }
}
