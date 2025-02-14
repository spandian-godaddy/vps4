package com.godaddy.vps4.web;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import com.godaddy.vps4.shopper.ShopperModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.godaddy.hfs.servicediscovery.HfsServiceMetadata;
import com.godaddy.hfs.servicediscovery.ZkServiceRegistrationModule;
import com.godaddy.hfs.swagger.SwaggerClassFilter;
import com.godaddy.hfs.swagger.SwaggerModule;
import com.godaddy.hfs.web.GuiceFilterModule;
import com.godaddy.hfs.web.HttpModule;
import com.godaddy.hfs.web.ServerModule;
import com.godaddy.hfs.zookeeper.ZooKeeperModule;
import com.godaddy.vps4.cache.CacheModule;
import com.godaddy.vps4.cache.HazelcastCacheModule;
import com.godaddy.vps4.cdn.CdnDataModule;
import com.godaddy.vps4.cdn.CdnModule;
import com.godaddy.vps4.cpanel.CpanelModule;
import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.entitlement.EntitlementsModule;
import com.godaddy.vps4.hfs.HfsClientModule;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordModule;
import com.godaddy.vps4.intent.IntentModule;
import com.godaddy.vps4.ipblacklist.IpBlacklistModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.jsd.JsdModule;
import com.godaddy.vps4.mailrelay.MailRelayModule;
import com.godaddy.vps4.messaging.MessagingModule;
import com.godaddy.vps4.move.VmMoveModule;
import com.godaddy.vps4.oh.OhModule;
import com.godaddy.vps4.panopta.PanoptaModule;
import com.godaddy.vps4.plan.PlanModule;
import com.godaddy.vps4.plesk.PleskModule;
import com.godaddy.vps4.prodMeta.ExternalDc.CrossDcProdMetaServiceModule;
import com.godaddy.vps4.productPackage.PackageModule;
import com.godaddy.vps4.reseller.ResellerModule;
import com.godaddy.vps4.scheduler.api.client.SchedulerServiceClientModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.shopperNotes.ShopperNotesModule;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.sso.SsoModule;
import com.godaddy.vps4.sso.Vps4SsoModule;
import com.godaddy.vps4.util.ObjectMapperProvider;
import com.godaddy.vps4.util.UtilsModule;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.customNotes.CustomNotesModule;
import com.godaddy.vps4.web.log.LogModule;
import com.godaddy.vps4.web.network.NetworkModule;
import com.godaddy.vps4.web.notification.NotificationsModule;
import com.godaddy.vps4.web.security.AuthenticationFilter;
import com.godaddy.vps4.web.security.GDUserModule;
import com.godaddy.vps4.web.security.Vps4CorsFilter;
import com.godaddy.vps4.web.util.ImportedVmFilter;
import com.godaddy.vps4.web.util.RequestIdFilter;
import com.godaddy.vps4.web.util.VmActiveSnapshotFilter;
import com.godaddy.vps4.web.vm.ServerUsageStatsModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.servlet.ServletModule;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.cluster.ClusterClientModule;
import gdg.hfs.orchestration.web.CommandsResource;
import gdg.hfs.orchestration.web.CommandsViewResource;

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
        HfsServiceMetadata metadata = new HfsServiceMetadata("vps4-web",
                HfsServiceMetadata.ServiceType.OTHER, "/api/", "/open/");
        modules.add(binder -> {
            binder.bind(HfsServiceMetadata.class).toInstance(metadata);
            OptionalBinder.newOptionalBinder(binder, HfsServiceMetadata.class);
        });

        modules.add(new ServerModule());
        modules.add(new HttpModule());
        modules.add(new GuiceFilterModule(
                "/",
                "/api/*",
                "/open/*",
                "/swagger.json"
        ));
        modules.add(new SwaggerModule());

        modules.add(new ZooKeeperModule());
        modules.add(new ZkServiceRegistrationModule());

        modules.add(new HfsClientModule());
        modules.add(new GDUserModule());
        modules.add(new DatabaseModule());
        modules.add(new WebModule());
        modules.add(new SecurityModule());
        modules.add(new SsoModule());
        modules.add(new UtilsModule());

        modules.add(new CreditModule());
        modules.add(new VmModule());
        modules.add(new HfsVmTrackingRecordModule());
        modules.add(new SnapshotModule());
        modules.add(new NetworkModule());
        modules.add(new MailRelayModule());
        modules.add(new CpanelModule());
        modules.add(new PleskModule());
        modules.add(new MessagingModule());
        modules.add(new EntitlementsModule());
        modules.add(new PlanModule());
        modules.add(new ServerUsageStatsModule());
        modules.add(new PanoptaModule());
        modules.add(new IpBlacklistModule());
        modules.add(new ShopperNotesModule());
        modules.add(new NotificationsModule());
        modules.add(new CdnDataModule());
        modules.add(new ResellerModule());
        modules.add(new CustomNotesModule());
        modules.add(new OhModule());
        modules.add(new JsdModule());
        modules.add(new Vps4SsoModule());
        modules.add(new CdnModule());
        modules.add(new ShopperModule());
        modules.add(new VmMoveModule());
        modules.add(new PackageModule());
        modules.add(new IntentModule());
        modules.add(new CrossDcProdMetaServiceModule());
        modules.add(binder -> {
            binder.bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);
        });

        logger.info("Orchestration engine clustered: {}", isOrchestrationEngineClustered);
        if(isOrchestrationEngineClustered) {
            logger.info("Using ClusterClientModule for orchestration engine.");
            modules.add(new PrivateModule() {
                @Override
                public void configure() {
                    install(new ClusterClientModule());

                    expose(CommandService.class);
                }
            });
        } else {
            modules.add(new CommandClientModule());
        }

        modules.add(new ServletModule() {
            @Override
            public void configureServlets() {

                bind(Vps4CorsFilter.class).in(Singleton.class);
                filter("/api/*").through(Vps4CorsFilter.class);
                filter("/open/*").through(Vps4CorsFilter.class);

                // attach a thread-local request ID
                bind(RequestIdFilter.class).in(Singleton.class);
                filter("/api/*").through(RequestIdFilter.class);
                filter("/open/*").through(RequestIdFilter.class);

                bind(AuthenticationFilter.class).in(Singleton.class);
                filter("/api/*").through(AuthenticationFilter.class);

                bind(VmActiveSnapshotFilter.class).in(Singleton.class);
                filter("/api/vms/*").through(VmActiveSnapshotFilter.class);

                bind(ImportedVmFilter.class).in(Singleton.class);
                filter("/api/vms/*").through(ImportedVmFilter.class);

                Multibinder.newSetBinder(binder(), SwaggerClassFilter.class)
                        .addBinding().toInstance(Vps4Injector::isResourceSwaggerVisible);
            }
        });
        modules.add(new LogModule());
        modules.add(new CacheModule());
        modules.add(new HazelcastCacheModule());
        modules.add(new SchedulerServiceClientModule());

        return Guice.createInjector(modules);
    }

    protected static boolean isResourceSwaggerVisible(Class<?> resourceClass) {

        boolean vps4Api = resourceClass.isAnnotationPresent(Vps4Api.class);

        boolean command = resourceClass.isAssignableFrom(CommandsResource.class)
                || resourceClass.isAssignableFrom(CommandsViewResource.class);

        return vps4Api || command;
    }

}
