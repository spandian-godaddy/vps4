package com.godaddy.vps4.consumer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import gdg.hfs.orchestration.jackson.ObjectMapperProvider;

public class Vps4ConsumerInjector {


//    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerInjector.class);

    private static final Injector INJECTOR = newInstance();

    public Injector getInstance() {
        return INJECTOR;
    }

    public static Injector newInstance() {
        List<Module> modules = new ArrayList<>();
        modules.add(new ConfigModule());
        modules.add(new VmModule());
        modules.add(new SnapshotModule());
        modules.add(new SecurityModule());
        modules.add(new DatabaseModule());
        modules.add(binder -> {binder.bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);});
        modules.add(new HfsClientModule());
        modules.add(new CreditModule());
        modules.add(new CommandClientModule());
        modules.add(new Vps4ConsumerModule());
        return Guice.createInjector(modules);
    }

}

