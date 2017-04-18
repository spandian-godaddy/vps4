package com.godaddy.vps4.consumer;

import java.util.ArrayList;
import java.util.List;

import com.godaddy.vps4.config.ConfigModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class Vps4ConsumerInjector {


//    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerInjector.class);

    private static final Injector INJECTOR = newInstance();

    public Injector getInstance() {
        return INJECTOR;
    }

    public static Injector newInstance() {
        List<Module> modules = new ArrayList<>();
        modules.add(new ConfigModule());
        modules.add(new Vps4ConsumerModule());
        return Guice.createInjector(modules);
    }

}

