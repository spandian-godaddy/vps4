package com.godaddy.vps4.shopperNotes;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ShopperNotesModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ShopperNotesService.class).to(DefaultShopperNotesService.class).in(Scopes.SINGLETON);
        bind(ShopperNotesClientService.class).to(DefaultShopperNotesClientService.class).in(Scopes.SINGLETON);
    }
}
