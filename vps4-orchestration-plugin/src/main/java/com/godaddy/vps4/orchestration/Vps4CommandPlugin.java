package com.godaddy.vps4.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.cache.HazelcastCacheModule;
import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.hfs.HfsVmTrackingRecordModule;
import com.godaddy.vps4.ipblacklist.IpBlacklistModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.monitoring.MonitoringModule;
import com.godaddy.vps4.orchestration.account.AccountModule;
import com.godaddy.vps4.orchestration.hfs.HfsCommandModule;
import com.godaddy.vps4.orchestration.hfs.HfsMockModule;
import com.godaddy.vps4.orchestration.hfs.HfsModule;
import com.godaddy.vps4.orchestration.scheduler.SchedulerModule;
import com.godaddy.vps4.panopta.PanoptaModule;
import com.godaddy.vps4.scheduler.api.client.SchedulerServiceClientModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.godaddy.vps4.util.UtilsModule;
import com.godaddy.vps4.vm.VmModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import gdg.hfs.orchestration.CommandPlugin;
import gdg.hfs.orchestration.CommandProvider;
import gdg.hfs.orchestration.GuiceCommandProvider;

import ch.qos.logback.classic.Level;

public class Vps4CommandPlugin implements CommandPlugin {

    private static final Logger logger = LoggerFactory.getLogger(Vps4CommandPlugin.class);

    @Override
    public String getName() {
        return "vps4";
    }

    @Override
    public void start() {
        Injector injector = Guice.createInjector(
            new ConfigModule()
        );

        Config config = injector.getInstance(Config.class);
        setLogLevel(config);
    }

    @Override
    public CommandProvider newCommandProvider() {

        
        AbstractModule hfsModule = null;
        
        if (System.getProperty("vps4.hfs.mock", "false").equals("true")) {
            hfsModule = new HfsMockModule();
            logger.info("USING MOCK HFS");
        }
        else{
            hfsModule = new HfsModule();
        }
        
        Injector injector = Guice.createInjector(
            new HazelcastCacheModule(),
            new ObjectMapperModule(),
            hfsModule,
            new HfsCommandModule(),
            new DatabaseModule(),
            new VmModule(),
            new CreditModule(),
            new SnapshotModule(),
            new Vps4CommandModule(),
            new UtilsModule(),
            new SchedulerServiceClientModule(),
            new SchedulerModule(),
            new AccountModule(),
            new SecurityModule(),
            new MonitoringModule(),
            new HfsVmTrackingRecordModule(),
            new PanoptaModule(),
            new IpBlacklistModule()
            );
            
        return new GuiceCommandProvider(injector);
    }

    private static void setLogLevel(Config config)
    {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        Level level = Level.toLevel(config.get("vps4.log.level.orchestration"), Level.INFO);
        root.setLevel(level);
    }

}
