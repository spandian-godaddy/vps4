package com.godaddy.vps4.consumer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.zookeeper.ZooKeeperModule;
import com.godaddy.vps4.backupstorage.BackupStorageModule;
import com.godaddy.vps4.config.ConfigModule;
import com.godaddy.vps4.credit.CreditModule;
import com.godaddy.vps4.hfs.HfsClientModule;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.messaging.MessagingModule;
import com.godaddy.vps4.orchestration.hfs.HfsMockModule;
import com.godaddy.vps4.panopta.PanoptaDataModule;
import com.godaddy.vps4.security.SecurityModule;
import com.godaddy.vps4.snapshot.SnapshotModule;
import com.godaddy.vps4.util.ObjectMapperModule;
import com.godaddy.vps4.vm.VmModule;
import com.godaddy.vps4.web.client.Vps4ApiWithCertAuthClientModule;
import com.godaddy.vps4.web.client.Vps4ApiWithSSOAuthClientModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import gdg.hfs.orchestration.cluster.ClusterClientModule;

public class Vps4ConsumerInjector {


    private static final Logger logger = LoggerFactory.getLogger(Vps4ConsumerInjector.class);

    private static final boolean isOrchestrationEngineClustered =
            Boolean.parseBoolean(System.getProperty("orchestration.engine.clustered", "true"));

    private static final Injector INJECTOR = newInstance();

    public static Injector newInstance() {
        List<Module> modules = new ArrayList<>();
        modules.add(binder -> {
            binder.requireExplicitBindings();
        });
        modules.add(new ObjectMapperModule());

        if (System.getProperty("vps4.hfs.mock", "false").equals("true")) {
            // the HFSMockModule also provides bindings for the messaging service
            logger.info("USING MOCK HFS");
            modules.add(new HfsMockModule());
        } else {
            modules.add(new MessagingModule());
            modules.add(new HfsClientModule());
        }

        if (Boolean.parseBoolean(System.getProperty("vps4.web.useJwtAuth", "false"))) {
            logger.info("Using the Vps4ApiWithSSOAuthClientModule and sso-jwt token.");
            modules.add(new Vps4ApiWithSSOAuthClientModule());
        } else {
            logger.info("Using the Vps4ApiWithCertAuthClientModule and certs.");
            modules.add(new Vps4ApiWithCertAuthClientModule(
                    "consumer.client.keyPath", "consumer.client.certPath"));
        }

        modules.add(new ConfigModule());
        modules.add(new VmModule());
        modules.add(new SnapshotModule());
        modules.add(new SecurityModule());
        modules.add(new DatabaseModule());
        modules.add(new CreditModule());
        modules.add(new PanoptaDataModule());

        logger.info("Orchestration engine clustered: {}", isOrchestrationEngineClustered);
        if (isOrchestrationEngineClustered) {
            logger.info("Using ClusterClientModule for orchestration engine.");
            // the zookeeper module is added here since it provides the CuratorFramework Binding
            // to the LeaderLatchProvider which is used in the ClusterModule bound to the ClusterClientModule
            modules.add(new ZooKeeperModule());
            modules.add(new ClusterClientModule());
        } else {
            modules.add(new CommandClientModule());
        }

        modules.add(new Vps4ConsumerModule());
        return Guice.createInjector(modules);
    }

    public Injector getInstance() {
        return INJECTOR;
    }

}

