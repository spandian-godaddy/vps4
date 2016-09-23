package com.godaddy.vps4.jdbc;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.godaddy.vps4.util.IOUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Sql implements AutoCloseable {

    private final Connection conn;
    private boolean transactionUncommitted = false;

    public Sql(Connection conn) {
        this.conn = conn;
    }

    public static Sql with(DataSource dataSource) {
        try {
            return new Sql(dataSource.getConnection());
        } catch (SQLException e) {
            throw new RuntimeException("Sql.with exception", e);
        }
    }

    public static Sql with(DataSource dataSource, String schemaName) {
        try {
            Connection conn = dataSource.getConnection();
            conn.setSchema(schemaName);
            return new Sql(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Sql.with exception", e);
        }
    }

    public Sql beginTrans() {
        try {
            conn.setAutoCommit(false);
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
        try {
            if (transactionUncommitted) {
                conn.rollback();
                transactionUncommitted = false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Sql.close exception", e);
        } finally {
            IOUtils.closeQuietly(conn);
        }
    }

    @FunctionalInterface
    public interface ResultSetMapper<ResultType> {
        ResultType map(ResultSet rs) throws SQLException;
    }

    public <ResultType> ResultType call(String sql, ResultSetMapper<ResultType> mapper, Object... params) {
        try {
            CallableStatement stmt = conn.prepareCall(sql);
            return execAndClose(mapper, stmt, params);
        } catch (SQLException e) {
            throw new RuntimeException("Sql.call exception", e);
        }
    }

    public <ResultType> ResultType exec(String sql, ResultSetMapper<ResultType> mapper, Object... params) {
        try {
            PreparedStatement stmt = conn.prepareStatement(sql);
            return execAndClose(mapper, stmt, params);
        } catch (SQLException e) {
            throw new RuntimeException("Sql.exec exception - " + e.getMessage(), e);
        }
    }

    private <ResultType> ResultType execAndClose(ResultSetMapper<ResultType> mapper, PreparedStatement stmt, Object[] params) throws SQLException {
        try {
            return exec(stmt, mapper, params);
        } finally {
            IOUtils.closeQuietly(stmt);
            if (!transactionUncommitted)
                IOUtils.closeQuietly(conn);
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
