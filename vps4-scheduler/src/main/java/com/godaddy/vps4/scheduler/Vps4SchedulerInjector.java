package com.godaddy.vps4.scheduler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.servicediscovery.HfsServiceMetadata;
import com.godaddy.hfs.servicediscovery.ZkServiceRegistrationModule;
import com.godaddy.hfs.swagger.SwaggerModule;
import com.godaddy.hfs.web.HttpModule;
import com.godaddy.hfs.web.ServerModule;
import com.godaddy.hfs.zookeeper.ZooKeeperModule;
import com.godaddy.vps4.scheduler.core.CoreModule;
import com.godaddy.vps4.scheduler.core.config.ConfigModule;
import com.godaddy.vps4.scheduler.core.quartz.jdbc.QuartzDatabaseModule;
import com.godaddy.vps4.scheduler.core.quartz.memory.QuartzMemoryModule;
import com.godaddy.vps4.scheduler.plugin.Vps4SchedulerPluginModule;
import com.godaddy.vps4.scheduler.security.AuthenticationFilter;
import com.godaddy.vps4.scheduler.security.SecurityModule;
import com.godaddy.vps4.scheduler.util.GuiceFilterModule;
import com.godaddy.vps4.scheduler.web.WebModule;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.servlet.ServletModule;

public class Vps4SchedulerInjector {

    private static final Logger logger = LoggerFactory.getLogger(Vps4SchedulerInjector.class);

    private static final Injector INJECTOR = newInstance();

    public Injector getInstance() {
        return INJECTOR;
    }

    public static Injector newInstance() {
        List<Module> modules = new ArrayList<>();

        /* use this when upgrading to newer version of hfs-web */
        HfsServiceMetadata metadata = new HfsServiceMetadata("vps4-scheduler", HfsServiceMetadata.ServiceType.OTHER, "/scheduler/");
        modules.add(binder -> {
            binder.requireExplicitBindings();
            binder.bind(HfsServiceMetadata.class).toInstance(metadata);
            OptionalBinder.newOptionalBinder(binder, HfsServiceMetadata.class);
        });

        modules.add(new ObjectMapperModule());
        modules.add(new ServerModule());
        modules.add(new HttpModule());
        modules.add(new GuiceFilterModule(
                "/",
                "/scheduler/*",
                "/swagger.json"
        ));
        modules.add(new SwaggerModule());
        modules.add(new SecurityModule());
        modules.add(new ZooKeeperModule());
        modules.add(new ZkServiceRegistrationModule());
        modules.add(new ConfigModule());


        if (System.getProperty("scheduler.jobstore.mode", "jdbc").equals("memory")) {
            logger.info("Using memory based job store");
            modules.add(new QuartzMemoryModule());
        } else {
            logger.info("Using JDBC based job store");
            modules.add(new QuartzDatabaseModule());
        }

        // Mutual auth is on by default and routes requests to the scheduler resource via the auth filter
        if (Boolean.parseBoolean(System.getProperty("scheduler.useMutualAuth", "true"))) {
            logger.info("Configuring mutual authentication");
            modules.add(new ServletModule() {
                @Override
                public void configureServlets() {
                    bind(AuthenticationFilter.class).in(Singleton.class);
                    filter("/scheduler/*").through(AuthenticationFilter.class);
                }
            });
        }

        modules.add(new CoreModule());
        modules.add(new SchedulerContextListenerModule());

        // this needs to be converted to an Service Loader plugin discovery style usage
        /*
        ServiceLoader<SchedulerPluginModule> extensions = ServiceLoader.load(SchedulerPluginModule.class);
        for(PluginModule ext : extensions) {
            modules.add(ext);         // add each found plugin module
        }
        */
        modules.add(new Vps4SchedulerPluginModule());

        modules.add(new WebModule());

        return Guice.createInjector(modules);
    }

}