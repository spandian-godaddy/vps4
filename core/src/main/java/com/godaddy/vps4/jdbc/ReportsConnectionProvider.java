package com.godaddy.vps4.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportsConnectionProvider implements Provider<Connection> {

    private static final Logger logger = LoggerFactory.getLogger(ReportsConnectionProvider.class);

    private final DataSource reportsDataSource;

    @Inject
    public ReportsConnectionProvider(@Vps4ReportsDataSource DataSource reportsDataSource) {
        this.reportsDataSource = reportsDataSource;
    }

    @Override
    public Connection get() {
        try {
            return reportsDataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Error acquiring database connection for the reportsDataSource", e);
            throw new RuntimeException(e);
        }
    }

}
