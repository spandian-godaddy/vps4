package com.godaddy.vps4.entitlement;

import com.godaddy.vps4.sso.CertJwtApi;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import static com.godaddy.vps4.client.ClientUtils.getCertJwtAuthServiceProvider;

public class EntitlementsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EntitlementsService.class).to(DefaultEntitlementsService.class).in(Singleton.class);
        bind(EntitlementsApiService.class)
                .toProvider(getCertJwtAuthServiceProvider(EntitlementsApiService.class,
                                                          "entitlements.api.url",
                                                          CertJwtApi.ENTITLEMENTS))
                .in(Singleton.class);
        bind(EntitlementsReadOnlyApiService.class)
                .toProvider(getCertJwtAuthServiceProvider(EntitlementsReadOnlyApiService.class,
                        "entitlements.api.readonly.url",
                        CertJwtApi.ENTITLEMENTS))
                .in(Singleton.class);
        bind(SubscriptionsShimApiService.class)
                .toProvider(getCertJwtAuthServiceProvider(SubscriptionsShimApiService.class,
                        "subscriptions.shim.readonly.api.url",
                        CertJwtApi.ENTITLEMENTS))
                .in(Singleton.class);
    }
}
