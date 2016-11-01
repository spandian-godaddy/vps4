package com.godaddy.vps4.web.hfs;

import javax.inject.Singleton;

import com.godaddy.vps4.hfs.HfsClientProvider;
import com.godaddy.vps4.hfs.VmService;
import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class HfsModule extends AbstractModule{

    @Override
    public void configure() {
        bind(SysAdminService.class).toProvider(new HfsClientProvider<SysAdminService>(SysAdminService.class)).in(Singleton.class);
        bind(VmService.class).toProvider(new HfsClientProvider<VmService>(VmService.class)).in(Singleton.class);
        bind(NetworkService.class).toProvider(new HfsClientProvider<NetworkService>(NetworkService.class)).in(Singleton.class);
    }
}