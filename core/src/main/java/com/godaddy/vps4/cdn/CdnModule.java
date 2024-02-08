package com.godaddy.vps4.cdn;

import com.godaddy.vps4.sso.CertJwtApi;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

import static com.godaddy.vps4.client.ClientUtils.getCertJwtAuthServiceProvider;

public class CdnModule extends AbstractModule {
    @Override
    public void configure() {
        bind(CdnService.class).to(DefaultCdnService.class).in(Singleton.class);
        bind(CdnClientService.class)
                .toProvider(getCertJwtAuthServiceProvider(CdnClientService.class,
                        "firewall.api.base.url",
                        CertJwtApi.CDN))
                .in(Singleton.class);
    }
}
