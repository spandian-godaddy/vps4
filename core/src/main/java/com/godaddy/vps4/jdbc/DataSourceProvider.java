package com.godaddy.vps4.jdbc;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSourceProvider implements Provider<DataSource> {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceProvider.class);

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
        String username = config.get("db.vps4.username");
        String password = config.get("db.vps4.password");
        String database = config.get("db.vps4.database");

        int port = Integer.parseInt(config.get("db.vps4.port"));

        String jdbcUrl = "jdbc:postgresql://" + server + ":" + port + "/" + database;;

        logger.info("building datasource to {}@{}", new Object[]{username, jdbcUrl});

        HikariConfig dsConfig = new HikariConfig();
        dsConfig.setJdbcUrl(jdbcUrl);
        dsConfig.setUsername(username);
        dsConfig.setPassword(password);

        final HikariDataSource dataSource = new HikariDataSource(dsConfig);;

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                logger.info("shutting down vertical database connection pool");
                dataSource.close();
            }
        }));
        return dataSource;
    }
}
