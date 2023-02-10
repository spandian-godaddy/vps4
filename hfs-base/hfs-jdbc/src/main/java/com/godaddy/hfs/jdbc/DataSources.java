package com.godaddy.hfs.jdbc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSources {

    private static final Logger logger = LoggerFactory.getLogger(DataSources.class);

    private static final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();

    private DataSources() {
        // defeat instantiation
    }

    public static DataSource getDataSource(String jdbcUrl, String username, String password) {

        String key = username + "@" + jdbcUrl;

        HikariDataSource dataSource = dataSources.computeIfAbsent(key, k -> {

            logger.info("building datasource to {}", key);

            HikariConfig dsConfig = new HikariConfig();
            dsConfig.setJdbcUrl(jdbcUrl);
            dsConfig.setUsername(username);
            dsConfig.setPassword(password);

            return new HikariDataSource(dsConfig);
        });

        return dataSource;
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {

                logger.info("closing all database connection pools");

                for (Map.Entry<String, HikariDataSource> dsEntry : dataSources.entrySet()) {

                    String url = dsEntry.getKey();
                    HikariDataSource dataSource = dsEntry.getValue();

                    logger.info("closing database connection pool: {}", url);
                    try {
                        dataSource.close();
                    } catch (Exception e) {
                        logger.error("Error closing database connection pool", e);
                    }
                }
            }
        }));
    }
}
