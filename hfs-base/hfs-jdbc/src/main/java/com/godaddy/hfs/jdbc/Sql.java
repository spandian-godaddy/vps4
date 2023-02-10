package com.godaddy.hfs.jdbc;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.godaddy.hfs.io.IOUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Sql implements AutoCloseable {

    private final DataSource dataSource;

    private final String schemaName;

    private volatile Connection conn;

    private boolean transactionUncommitted = false;

    public Sql(DataSource dataSource) {
        this(dataSource, null);
    }

    public Sql(DataSource dataSource, String schemaName) {
        this.dataSource = dataSource;
        this.schemaName = schemaName;
    }

    public Sql(Connection conn) {
        this.conn = conn;
        this.dataSource = null;
        this.schemaName = null;
    }

    public Connection getConnection() throws SQLException {
        if (conn != null) {
            return conn;
        }
        conn = dataSource.getConnection();
        if (this.schemaName != null) {
            conn.setSchema(schemaName);
        }
        return conn;
    }

    public static Sql with(DataSource dataSource) {
        return new Sql(dataSource);
    }

    public static Sql with(DataSource dataSource, String schemaName) {
        return new Sql(dataSource, schemaName);
    }

    public Sql beginTrans() {
        try {
            getConnection().setAutoCommit(false);
            transactionUncommitted = true;
            return this;    // Return this so that it can be used in a Try-with-resources
        } catch (SQLException e) {
            throw new RuntimeException("Sql.beginTrans exception", e);
        }
    }

    public void rollback() {
        try {
            if (transactionUncommitted) {
                conn.rollback();
                transactionUncommitted = false;
            } else {
                throw new IllegalStateException("rollback outside transaction");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Sql.rollback exception", e);
        }
    }

    public void commit() {
        try {
            if (transactionUncommitted) {
                conn.commit();
                transactionUncommitted = false;
            } else {
                throw new IllegalStateException("commit outside transaction");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (conn == null) {
            return;
        }

        try {
            if (transactionUncommitted) {
                conn.rollback();
                transactionUncommitted = false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Sql.close exception", e);

        } finally {
            IOUtils.closeQuietly(conn);
            conn = null;
        }
    }

    protected void maybeClose() {
        if (dataSource == null) {
            //  we were assigned a connection externally,
            //  so don't close it automatically
            return;
        }
        if (!transactionUncommitted) {
            // if we're managing the connection (from a DataSource)
            // and the transaction has been committed (or was never explicitly started)
            // then we can close the connection
            IOUtils.closeQuietly(conn);
            conn = null;
        }
    }

    @FunctionalInterface
    public interface ResultSetMapper<ResultType> {
        ResultType map(ResultSet rs) throws SQLException;
    }

    public <ResultType> ResultType call(String sql, ResultSetMapper<ResultType> mapper, Object... params) {
        try {
            Connection conn = getConnection();
            try (CallableStatement stmt = conn.prepareCall(sql)) {

                return exec(stmt, mapper, params);

            } finally {
                maybeClose();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Sql.call exception", e);
        }
    }

    public <ResultType> ResultType exec(String sql, ResultSetMapper<ResultType> mapper, Object... params) {
        try {
            Connection conn = getConnection();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                return exec(stmt, mapper, params);

            } finally {
                maybeClose();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Sql.exec exception - " + e.getMessage(), e);

        }
    }

    public <ResultType> ResultType exec(PreparedStatement stmt, ResultSetMapper<ResultType> mapper, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                stmt.setNull(i + 1, Types.NULL); // Doing this explicitly to support UUID which setObject doesn't handle well
            } else {
                stmt.setObject(i + 1, params[i]);
            }
        }

        // expecting result set, so pass to mapper
        if (mapper != null) {
            return mapper.map(stmt.executeQuery());
        }

        // no mapper specified, so no resultset specified, so just execute and return
        stmt.execute();

        return null;
    }

    public static <ResultType> ResultSetMapper<ResultType> nextOrNull(final ResultSetMapper<ResultType> mapper) {
        return (rs) -> {
            if (rs.next()) {
                return mapper.map(rs);
            }
            return null;
        };
    }

    public static <ResultType> ResultSetMapper<List<ResultType>> listOf(final ResultSetMapper<ResultType> mapper) {
        return (rs) -> {
            List<ResultType> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapper.map(rs));
            }
            return list;
        };
    }

    public static <ResultType> ResultSetMapper<Set<ResultType>> setOf(final ResultSetMapper<ResultType> mapper) {
        return (rs) -> {
            Set<ResultType> set = new HashSet<>();
            while (rs.next()) {
                set.add(mapper.map(rs));
            }
            return set;
        };
    }

    public static <KeyType, ResultType> Sql.ResultSetMapper<Map<KeyType, ResultType>> mapOf(final Sql.ResultSetMapper<ResultType> dataMapper, final Sql.ResultSetMapper<KeyType> keyMapper) {
        return (rs) -> {
            Map<KeyType, ResultType> map = new HashMap<>();
            while (rs.next()) {
                map.put(keyMapper.map(rs), dataMapper.map(rs));
            }
            return map;
        };
    }
}

