package com.godaddy.vps4.orchestration.hfs;

import javax.inject.Singleton;

import com.godaddy.hfs.cpanel.CPanelService;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.hfs.HfsClientProvider;
import com.godaddy.vps4.messaging.MessagingProvider;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.network.NetworkServiceV2;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.snapshot.SnapshotService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class HfsModule extends AbstractModule {

    @Override
    public void configure() {
        bind(SysAdminService.class).toProvider(new HfsClientProvider<SysAdminService>(SysAdminService.class)).in(Singleton.class);
        bind(VmService.class).toProvider(new HfsClientProvider<VmService>(VmService.class)).in(Singleton.class);
        bind(NetworkServiceV2.class).toProvider(new HfsClientProvider<NetworkServiceV2>(NetworkServiceV2.class)).in(Singleton.class);
        bind(CPanelService.class).toProvider(new HfsClientProvider<CPanelService>(CPanelService.class)).in(Singleton.class);
        bind(PleskService.class).toProvider(new HfsClientProvider<PleskService>(PleskService.class)).in(Singleton.class);
        bind(MailRelayService.class).toProvider(new HfsClientProvider<MailRelayService>(MailRelayService.class)).in(Singleton.class);
        bind(ECommService.class).toProvider(new HfsClientProvider<ECommService>(ECommService.class)).in(Singleton.class);
        bind(NodePingService.class).toProvider(new HfsClientProvider<NodePingService>(NodePingService.class)).in(Singleton.class);
        bind(SnapshotService.class).toProvider(new HfsClientProvider<>(SnapshotService.class)).in(Singleton.class);
        bind(Vps4MessagingService.class).toProvider(MessagingProvider.class).in(Singleton.class);
        bind(HfsDnsService.class).toProvider(new HfsClientProvider<HfsDnsService>(HfsDnsService.class)).in(Singleton.class);
    }
}
