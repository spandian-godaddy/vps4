package com.godaddy.vps4.scheduler.core.quartz.jdbc;

import com.godaddy.hfs.config.Config;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import org.quartz.impl.jdbcjobstore.InvalidConfigurationException;
import org.quartz.impl.jdbcjobstore.JobStoreTX;
import org.quartz.spi.JobStore;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class QuartzDatabaseModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(QuartzDatabaseModule.class);

    @Override
    protected void configure() {
        bind(DataSource.class).toProvider(QuartzJobStoreDataSourceProvider.class).in(Scopes.SINGLETON);
        bind(ConnectionProvider.class).to(QuartzDBConnectionProvider.class).in(Scopes.SINGLETON);
        bindConstant()
            .annotatedWith(DBDriverDelegateClass.class)
            .to("org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        bindConstant().annotatedWith(DataSourceName.class).to("quartz:db");
    }

    @Singleton
    @Provides
    @Inject
    public JobStore getDBJobStore(Config config,
                                  ConnectionProvider connectionProvider,
                                  @DBDriverDelegateClass String delegateClass,
                                  @DataSourceName String dataSourceName) {
        JobStoreTX jobStore = new JobStoreTX();
        jobStore.setDataSource(dataSourceName);
        jobStore.setTablePrefix(config.get("db.vps4.scheduler.tablePrefix"));
        DBConnectionManager.getInstance().addConnectionProvider(dataSourceName, connectionProvider);
        try {
            jobStore.setDriverDelegateClass(delegateClass);
        } catch (InvalidConfigurationException e) {
            logger.error("Error acquiring database connection", e);
            throw new RuntimeException(e);
        }

        return jobStore;
    }
}
