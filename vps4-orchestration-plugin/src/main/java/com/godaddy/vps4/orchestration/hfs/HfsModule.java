package com.godaddy.vps4.orchestration.hfs;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.godaddy.vps4.hfs.HfsClientProvider;
import com.google.inject.AbstractModule;

import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.mailrelay.MailRelayService;
import gdg.hfs.vhfs.network.NetworkService;
import gdg.hfs.vhfs.sysadmin.SysAdminService;
import gdg.hfs.vhfs.vm.VmService;

public class HfsModule extends AbstractModule {

    @Override
    public void configure() {
        bind(SysAdminService.class).toProvider(new HfsClientProvider<SysAdminService>(SysAdminService.class)).in(Singleton.class);
        bind(VmService.class).toProvider(new HfsClientProvider<VmService>(VmService.class)).in(Singleton.class);
        bind(NetworkService.class).toProvider(new HfsClientProvider<NetworkService>(NetworkService.class)).in(Singleton.class);
        bind(CPanelService.class).toProvider(new HfsClientProvider<CPanelService>(CPanelService.class)).in(Singleton.class);
        bind(MailRelayService.class).toProvider(new HfsClientProvider<MailRelayService>(MailRelayService.class)).in(Singleton.class);
        
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