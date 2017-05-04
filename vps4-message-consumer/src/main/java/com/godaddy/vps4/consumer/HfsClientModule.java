package com.godaddy.vps4.consumer;

import javax.inject.Singleton;

import com.godaddy.vps4.hfs.HfsClientProvider;
import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.ecomm.ECommService;

public class HfsClientModule extends AbstractModule {

    @Override
    public void configure() {
        bind(ECommService.class).toProvider(new HfsClientProvider<ECommService>(ECommService.class)).in(Singleton.class);
    }
}