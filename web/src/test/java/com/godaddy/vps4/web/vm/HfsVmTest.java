package com.godaddy.vps4.web.vm;

import org.junit.Test;

import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.orchestration.hfs.HfsModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

public class HfsVmTest {

    @Test
    public void asdf() {
        Injector injector = Guice.createInjector(new HfsModule(), new ConfigModule(), new AbstractModule() {
            @Override
            public void configure() {
                binder().requireExplicitBindings();

//                ObjectMapper mapper = new ObjectMapper();
//                mapper.registerModule(new JSR310Module());
//                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

//                JacksonJsonProvider jsonProvider = new JacksonJaxbJsonProvider(mapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
//                jsonProvider.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//                bind(JacksonJsonProvider.class).toInstance(jsonProvider);
            }
        });

        VmService vmService = injector.getInstance(VmService.class);

        VmAction action = vmService.getVmAction(233, 836);

        System.out.println("action: " + action);


    }
}
