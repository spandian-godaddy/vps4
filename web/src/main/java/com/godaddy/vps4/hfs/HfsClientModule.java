package com.godaddy.vps4.hfs;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.vm.VmService;

public class HfsClientModule extends AbstractModule {

    @Override
    public void configure() {
        bind(VmService.class).toProvider(new HfsClientProvider<>(VmService.class)).in(Singleton.class);
        bind(CPanelService.class).toProvider(new HfsClientProvider<>(CPanelService.class)).in(Singleton.class);
        bind(PleskService.class).toProvider(new HfsClientProvider<>(PleskService.class)).in(Singleton.class);
    }
}
