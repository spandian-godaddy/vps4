package com.godaddy.vps4.scheduler.core.utils;

import com.godaddy.hfs.jdbc.DataSources;
import javax.sql.DataSource;

abstract public class DataSourceProvider {

    private final Object createLock = new Object();
    private final String server;
    private final String database;
    private final int port;
    private final String username;
    private final String password;

    public DataSourceProvider(String server, String database, int port, String username, String password) {
        this.server = server;
        this.database = database;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    private volatile DataSource dataSource;

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
        String jdbcUrl = "jdbc:postgresql://" + server + ":" + port + "/" + database;
        return DataSources.getDataSource(jdbcUrl, username, password);
    }


}
