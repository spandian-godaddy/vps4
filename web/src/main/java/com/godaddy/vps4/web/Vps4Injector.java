package com.godaddy.vps4.web;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.servicediscovery.HfsServiceMetadata;
import com.godaddy.hfs.servicediscovery.ZkServiceRegistrationModule;
import com.godaddy.hfs.swagger.SwaggerClassFilter;
import com.godaddy.hfs.swagger.SwaggerModule;
import com.godaddy.hfs.web.CorsFilter;
import com.godaddy.hfs.web.GuiceFilterModule;
import com.godaddy.hfs.web.HttpModule;
import com.godaddy.hfs.web.ServerModule;
import com.godaddy.hfs.zookeeper.ZooKeeperModule;
import com.godaddy.vps4.cache.CacheModule;
import com.godaddy.vps4.cache.HazelcastCacheModule;
import com.godaddy.vps4.cpanel.CpanelModule;
import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.hfs.HfsClientModule;
import com.godaddy.vps4.hfs.HfsMockModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.mailrelay.MailRelayModule;
import com.godaddy.vps4.messaging.MessagingModule;
import com.godaddy.vps4.plesk.PleskModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.sso.SsoModule;
import com.godaddy.vps4.sysadmin.SysAdminModule;
import com.godaddy.vps4.util.ObjectMapperProvider;
import com.godaddy.vps4.util.UtilsModule;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.network.NetworkModule;
import com.godaddy.vps4.web.security.AuthenticationFilter;
import com.godaddy.vps4.web.security.GDUserModule;
import com.godaddy.vps4.web.util.RequestIdFilter;
import com.godaddy.vps4.web.util.VmActiveSnapshotFilter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.servlet.ServletModule;
import gdg.hfs.orchestration.cluster.ClusterClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Vps4Injector {

    private static final Logger logger = LoggerFactory.getLogger(Vps4Injector.class);

    private static final boolean isOrchestrationEngineClustered =  Boolean.parseBoolean(System.getProperty("orchestration.engine.clustered", "true"));

    private static final Injector INJECTOR = newInstance();

    public Injector getInstance() {
        return INJECTOR;
    }

    static Injector newInstance() {
        List<Module> modules = new ArrayList<>();
        // use this when upgrading to newer version of hfs-web
        HfsServiceMetadata metadata = new HfsServiceMetadata("vps4-web", HfsServiceMetadata.ServiceType.OTHER, "/api/");
        modules.add(binder -> {
            binder.bind(HfsServiceMetadata.class).toInstance(metadata);
            OptionalBinder.newOptionalBinder(binder, HfsServiceMetadata.class);
        });

        modules.add(new ServerModule());
        modules.add(new HttpModule());
        modules.add(new GuiceFilterModule(
                "/api/*",
                "/",
                "/swagger.json"
        ));
        modules.add(new SwaggerModule());

        modules.add(new ZooKeeperModule());
        modules.add(new ZkServiceRegistrationModule());

        if (System.getProperty("vps4.hfs.mock", "false").equals("true")) {
            modules.add(new HfsMockModule());
            logger.info("USING MOCK HFS");
        } else {
            modules.add(new HfsClientModule());
        }

        modules.add(new GDUserModule());
        modules.add(new DatabaseModule());
        modules.add(new WebModule());
        modules.add(new SecurityModule());
        modules.add(new SsoModule());
        modules.add(new UtilsModule());

        modules.add(new CreditModule());
        modules.add(new VmModule());
        modules.add(new SnapshotModule());
        modules.add(new NetworkModule());
        modules.add(new SysAdminModule());
        modules.add(new MailRelayModule());
        modules.add(new CpanelModule());
        modules.add(new PleskModule());
        modules.add(new MessagingModule());
        modules.add(binder -> {
            binder.bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);
        });

        logger.info("Orchestration engine clustered: {}", isOrchestrationEngineClustered);
        if(isOrchestrationEngineClustered) {
            logger.info("Using ClusterClientModule for orchestration engine.");
            modules.add(new ClusterClientModule());
        } else {
            modules.add(new CommandClientModule());
        }

        modules.add(new ServletModule() {
            @Override
            public void configureServlets() {

                bind(CorsFilter.class).in(Singleton.class);
                filter("/api/*").through(CorsFilter.class);

                // attach a thread-local request ID
                bind(RequestIdFilter.class).in(Singleton.class);
                filter("/api/*").through(RequestIdFilter.class);

                bind(AuthenticationFilter.class).in(Singleton.class);
                filter("/api/*").through(AuthenticationFilter.class);

                bind(VmActiveSnapshotFilter.class).in(Singleton.class);
                filter("/api/vms/*").through(VmActiveSnapshotFilter.class);

                Multibinder.newSetBinder(binder(), SwaggerClassFilter.class)
                        .addBinding().toInstance(resourceClass ->
                        resourceClass.isAnnotationPresent(Vps4Api.class));
            }
        });
        modules.add(new CacheModule());
        modules.add(new HazelcastCacheModule());

        return Guice.createInjector(modules);
    }
}
