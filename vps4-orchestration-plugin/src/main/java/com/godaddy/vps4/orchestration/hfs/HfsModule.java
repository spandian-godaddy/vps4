package com.godaddy.vps4.orchestration.hfs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.vps4.hfs.HfsClientProvider;
import com.godaddy.vps4.messaging.MessagingProvider;
import com.godaddy.vps4.messaging.Vps4MessagingService;
import com.google.inject.AbstractModule;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.ecomm.ECommService;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.vm.VmService;
import gdg.hfs.vhfs.snapshot.SnapshotService;

import javax.inject.Singleton;

public class HfsModule extends AbstractModule {

    @Override
    public void configure() {
        bind(SysAdminService.class).toProvider(new HfsClientProvider<SysAdminService>(SysAdminService.class)).in(Singleton.class);
        bind(VmService.class).toProvider(new HfsClientProvider<VmService>(VmService.class)).in(Singleton.class);
        bind(NetworkService.class).toProvider(new HfsClientProvider<NetworkService>(NetworkService.class)).in(Singleton.class);
        bind(CPanelService.class).toProvider(new HfsClientProvider<CPanelService>(CPanelService.class)).in(Singleton.class);
        bind(PleskService.class).toProvider(new HfsClientProvider<PleskService>(PleskService.class)).in(Singleton.class);
        bind(MailRelayService.class).toProvider(new HfsClientProvider<MailRelayService>(MailRelayService.class)).in(Singleton.class);
        bind(ECommService.class).toProvider(new HfsClientProvider<ECommService>(ECommService.class)).in(Singleton.class);
        bind(NodePingService.class).toProvider(new HfsClientProvider<NodePingService>(NodePingService.class)).in(Singleton.class);
        bind(SnapshotService.class).toProvider(new HfsClientProvider<>(SnapshotService.class)).in(Singleton.class);
        bind(Vps4MessagingService.class).toProvider(MessagingProvider.class).in(Singleton.class);

        // hook Jackson into Jersey as the POJO <-> JSON mapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JSR310Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        JacksonJsonProvider jsonProvider = new JacksonJaxbJsonProvider(mapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
//      jsonProvider.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        bind(JacksonJsonProvider.class).toInstance(jsonProvider);
    }
}
