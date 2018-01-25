package com.godaddy.vps4.jdbc;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import com.godaddy.hfs.config.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportsDataSourceProvider implements Provider<DataSource> {

    private static final Logger logger = LoggerFactory.getLogger(ReportsDataSourceProvider.class);

    private final Config config;

    private static volatile DataSource reportsDataSource;

    private final Object createReportsDataSourceLock = new Object();

    @Inject
    public ReportsDataSourceProvider(Config config) {
        this.config = config;
    }

    @Override
    public DataSource get() {
        if (reportsDataSource == null) {
            synchronized (createReportsDataSourceLock) {
                if (reportsDataSource == null) {
                    reportsDataSource = buildReportsDataSource();
                }
            }
        }
        logger.info("Returning Reports Data Source.");
        return reportsDataSource;
    }

    private DataSource buildReportsDataSource() {

        logger.info("Building Reports Data Source.");

        // build datasource from config
        String server = config.get("db.vps4.server");
        String database = config.get("db.vps4.database");
        int port = Integer.parseInt(config.get("db.vps4.port"));
        String jdbcUrl = "jdbc:postgresql://" + server + ":" + port + "/" + database;
        String username = config.get("db.vps4.username");
        String password = config.get("db.vps4.password");

        HikariConfig hikariDsConfig = new HikariConfig();
        hikariDsConfig.setJdbcUrl(jdbcUrl);
        hikariDsConfig.setUsername(username);
        hikariDsConfig.setPassword(password);

        return new HikariDataSource(hikariDsConfig);
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("closing database connection pool for the reportsDataSource");
            try {
                reportsDataSource.unwrap(HikariDataSource.class).close();
            } catch (Exception ex) {
                logger.error("Error closing database connection pool for reportsDataSource ", ex);
            }
        }));

    }
}