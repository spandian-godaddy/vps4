package com.godaddy.vps4.plesk;

import com.godaddy.vps4.util.Vps4Poller;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;

import gdg.hfs.vhfs.plesk.PleskAction;

public class PleskModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Vps4PleskService.class).to(DefaultVps4PleskService.class).in(Scopes.SINGLETON);
        bind(new TypeLiteral<Vps4Poller<PleskAction, Integer, String>>(){}).to(Vps4PleskActionPoller.class);
    }
}
