package com.godaddy.vps4.jdbc;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import com.godaddy.hfs.config.Config;

public class DataSourceProvider implements Provider<DataSource> {

    private final Config config;

    private volatile DataSource dataSource;

    private final Object createLock = new Object();

    @Inject
    public DataSourceProvider(Config config) {
        this.config = config;
    }

    @Override
    public DataSource get() {
        if (dataSource == null) {
            synchronized(createLock) {
                if (dataSource == null) {
                    dataSource = buildDataSource();
                }
            }
        }
        return dataSource;
    }

    protected DataSource buildDataSource() {
        // build datasource from config
        String server = config.get("db.vps4.server");
        String database = config.get("db.vps4.database");
        int port = Integer.parseInt(config.get("db.vps4.port"));

        String jdbcUrl = "jdbc:postgresql://" + server + ":" + port + "/" + database;

        String username = config.get("db.vps4.username");
        String password = config.get("db.vps4.password");

        return DataSources.getDataSource(jdbcUrl, username, password);
    }


}
