package com.godaddy.vps4.shopper;

import com.godaddy.vps4.sso.CertJwtApi;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import static com.godaddy.vps4.client.ClientUtils.getCertJwtAuthServiceProvider;

public class ShopperModule extends AbstractModule {
    @Override
    public void configure() {
        bind(ShopperService.class).to(DefaultShopperService.class).in(Singleton.class);
        bind(ShopperApiService.class)
                .toProvider(getCertJwtAuthServiceProvider(ShopperApiService.class,
                        "shopper.api.url",
                        CertJwtApi.SHOPPER))
                .in(Singleton.class);
    }
}
