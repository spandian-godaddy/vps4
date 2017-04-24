package com.godaddy.vps4.consumer;

import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.ArrayList;
import java.util.List;

public class Vps4ConsumerInjector {


//    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerInjector.class);

    private static final Injector INJECTOR = newInstance();

    public Injector getInstance() {
        return INJECTOR;
    }

    public static Injector newInstance() {
        List<Module> modules = new ArrayList<>();
        modules.add(new ConfigModule());
        modules.add(new CreditModule());
        modules.add(new VmModule());
        modules.add(new SecurityModule());
        modules.add(new DatabaseModule());
        modules.add(new CommandClientModule());
        modules.add(new Vps4ConsumerModule());
        return Guice.createInjector(modules);
    }

}

