package com.godaddy.vps4.hfs;

import static com.godaddy.vps4.client.ClientUtils.getCertJwtAuthServiceProvider;

import com.godaddy.hfs.cpanel.CPanelService;
import com.godaddy.hfs.dns.HfsDnsService;
import com.godaddy.hfs.mailrelay.MailRelayService;
import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.sso.CertJwtApi;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.network.NetworkServiceV2;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.snapshot.SnapshotService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;

public class HfsClientModule extends AbstractModule {
    @Override
    public void configure() {
        bind(VmService.class)
                .toProvider(getCertJwtAuthServiceProvider(VmService.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
        bind(CPanelService.class)
                .toProvider(getCertJwtAuthServiceProvider(CPanelService.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
        bind(PleskService.class)
                .toProvider(getCertJwtAuthServiceProvider(PleskService.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
        bind(SysAdminService.class)
                .toProvider(getCertJwtAuthServiceProvider(SysAdminService.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
        bind(MailRelayService.class)
                .toProvider(getCertJwtAuthServiceProvider(MailRelayService.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
        bind(ECommService.class)
                .toProvider(getCertJwtAuthServiceProvider(ECommService.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
        bind(NetworkServiceV2.class)
                .toProvider(getCertJwtAuthServiceProvider(NetworkServiceV2.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
        bind(HfsDnsService.class)
                .toProvider(getCertJwtAuthServiceProvider(HfsDnsService.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
        bind(SnapshotService.class)
                .toProvider(getCertJwtAuthServiceProvider(SnapshotService.class, "hfs.base.url", CertJwtApi.HFS))
                .in(Singleton.class);
    }
}
