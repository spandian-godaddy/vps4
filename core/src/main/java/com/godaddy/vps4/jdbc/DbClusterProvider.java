package com.godaddy.vps4.jdbc;

import javax.inject.Inject;
import javax.inject.Provider;

import com.godaddy.hfs.config.Config;

public class DbClusterProvider implements Provider<DatabaseCluster> {
    private final Config config;

    private volatile DatabaseCluster cluster;

    private final Object createLock = new Object();

    @Inject
    public DbClusterProvider(Config config) {
        this.config = config;
    }

    @Override
    public DatabaseCluster get() {
        if (cluster == null) {
            synchronized(createLock) {
                if (cluster == null) {
                    cluster = buildDbCluster();
                }
            }
        }
        return cluster;
    }

    protected DatabaseCluster buildDbCluster() {
        String nodes = config.get("vps4.replication.monitoring.nodes", "");
        String database = config.get("db.vps4.database");
        int port = Integer.parseInt(config.get("db.vps4.port"));
        String username = config.get("db.vps4.username");
        String password = config.get("db.vps4.password");

        DatabaseCluster builder = new DatabaseCluster();
        if (!nodes.isEmpty()) {
            for (String server : nodes.split(";")) {
                String jdbcUrl = "jdbc:postgresql://" + server + ":" + port + "/" + database;
                builder.addServer(server, jdbcUrl, username, password);
            }
        }
        return builder;
    }
}
