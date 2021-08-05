package com.godaddy.vps4.appmonitors.jdbc;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.appmonitors.ReplicationLagService;
import com.godaddy.vps4.jdbc.DatabaseCluster;

public class JdbcReplicationLagService implements ReplicationLagService {
    private final DatabaseCluster databaseCluster;

    @Inject
    public JdbcReplicationLagService(DatabaseCluster databaseCluster) {
        this.databaseCluster = databaseCluster;
    }

    @Override
    public boolean isMasterServer(String server) {
        DataSource dataSource = databaseCluster.getServer(server);
        return Sql.with(dataSource).exec("SELECT pg_is_in_recovery() AS result",
                                         Sql.nextOrNull(rs -> rs.getString("result")))
                .equals("f");
    }

    @Override
    public String getCurrentLocation(String server) {
        DataSource dataSource = databaseCluster.getServer(server);
        return Sql.with(dataSource).exec("SELECT pg_current_xlog_location() AS result",
                                         Sql.nextOrNull(rs -> rs.getString("result")));
    }

    @Override
    public String getLastReceiveLocation(String server) {
        DataSource dataSource = databaseCluster.getServer(server);
        return Sql.with(dataSource).exec("SELECT pg_last_xlog_receive_location() AS result",
                                         Sql.nextOrNull(rs -> rs.getString("result")));
    }

    @Override
    public long comparePgLsns(String server, String lsn1, String lsn2) {
        DataSource dataSource = databaseCluster.getServer(server);
        return Sql.with(dataSource).exec("SELECT ?::pg_lsn - ?::pg_lsn AS result",
                                         Sql.nextOrNull(rs -> rs.getLong("result")), lsn1, lsn2);
    }
}
