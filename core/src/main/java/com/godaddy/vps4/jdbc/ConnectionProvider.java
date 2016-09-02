package com.godaddy.vps4.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionProvider implements Provider<Connection> {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionProvider.class);

    private final DataSource dataSource;

    @Inject
    public ConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection get() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Error acquiring database connection", e);
            throw new RuntimeException(e);
        }
    }

}
