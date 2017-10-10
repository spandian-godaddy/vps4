package com.godaddy.vps4.scheduler.core.quartz.jdbc;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.scheduler.core.utils.DataSourceProvider;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

public class QuartzJobStoreDataSourceProvider extends DataSourceProvider implements Provider<DataSource> {

    @Inject
    public QuartzJobStoreDataSourceProvider(Config config) {
        super(
            config.get("db.vps4.scheduler.server"),
            config.get("db.vps4.scheduler.database"),
            Integer.parseInt(config.get("db.vps4.scheduler.port")),
            config.get("db.vps4.scheduler.username"),
            config.get("db.vps4.scheduler.password"));
    }

    @Override
    public DataSource get() {
        return super.get();
    }
}
