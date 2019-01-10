package com.godaddy.vps4.hfs;

import javax.inject.Singleton;

import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.vm.VmService;
import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.network.NetworkServiceV2;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class HfsClientModule extends AbstractModule {

    @Override
    public void configure() {
        bind(VmService.class).toProvider(new HfsClientProvider<>(VmService.class)).in(Singleton.class);
        bind(CPanelService.class).toProvider(new HfsClientProvider<>(CPanelService.class)).in(Singleton.class);
        bind(PleskService.class).toProvider(new HfsClientProvider<>(PleskService.class)).in(Singleton.class);
        bind(SysAdminService.class).toProvider(new HfsClientProvider<>(SysAdminService.class)).in(Singleton.class);
        bind(MailRelayService.class).toProvider(new HfsClientProvider<>(MailRelayService.class)).in(Singleton.class);
        bind(ECommService.class).toProvider(new HfsClientProvider<>(ECommService.class)).in(Singleton.class);
        bind(NodePingService.class).toProvider(new HfsClientProvider<>(NodePingService.class)).in(Singleton.class);
        bind(NetworkServiceV2.class).toProvider(new HfsClientProvider<>(NetworkServiceV2.class)).in(Singleton.class);
    }
}
