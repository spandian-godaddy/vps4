package com.godaddy.vps4.jdbc;

import java.sql.Connection;

import javax.sql.DataSource;

import com.godaddy.vps4.config.ConfigModule;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class DatabaseModule extends AbstractModule {

    @Override
    public void configure() {
        install(new ConfigModule());
        bind(DataSource.class).toProvider(DataSourceProvider.class).in(Scopes.SINGLETON);
        bind(Connection.class).toProvider(ConnectionProvider.class);
        bind(DataSource.class).annotatedWith(Vps4ReportsDataSource.class).toProvider(ReportsDataSourceProvider.class).in(Scopes.SINGLETON);
        bind(Connection.class).annotatedWith(Vps4ReportsDataSource.class).toProvider(ReportsConnectionProvider.class);
    }

}
