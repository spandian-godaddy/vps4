package com.godaddy.vps4.scheduler.core.quartz.jdbc;

import com.google.inject.Inject;
import org.quartz.utils.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class QuartzDBConnectionProvider implements ConnectionProvider {

    private static final Logger logger = LoggerFactory.getLogger(QuartzDBConnectionProvider.class);

    private final DataSource dataSource;

    @Inject
    public QuartzDBConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Error acquiring database connection", e);
            throw e;
        }
    }

    @Override
    public void shutdown() throws SQLException {
        // do nothing as hfs 'DataSources' library takes care of calling close on the datasource
    }

    @Override
    public void initialize() throws SQLException {
        // do nothing, datasource injected into constructor already by DI
    }
}
