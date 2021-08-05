package com.godaddy.vps4.jdbc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.DataSources;

public class DatabaseCluster {
    private final Map<String, DataSource> map = new HashMap<>();

    public void addServer(String hostname, String jdbcUrl, String username, String password) {
        map.put(hostname, DataSources.getDataSource(jdbcUrl, username, password));
    }

    public DataSource getServer(String hostname) {
        return map.get(hostname);
    }

    public Set<String> getServers() {
        return new HashSet<>(map.keySet());
    }
}
