package com.godaddy.vps4.vm.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;
import com.godaddy.vps4.snapshot.SnapshotWithDetails;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public class JdbcSnapshotService implements SnapshotService {
    private final DataSource dataSource;
    private String selectSnapshotQuery = "SELECT s.id, s.hfs_image_id, s.project_id, s.hfs_snapshot_id, s.vm_id, s.name, ss.status, s.created_at, s.modified_at FROM snapshot s JOIN snapshot_status ss ON s.status = ss.status_id ";

    @Inject
    public JdbcSnapshotService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Snapshot> getSnapshotsForUser(long vps4UserId) {
        return Sql.with(dataSource).exec(selectSnapshotQuery
                        + "JOIN user_project_privilege up ON up.project_id = s.project_id "
                        + "JOIN vps4_user u ON u.vps4_user_id = up.vps4_user_id "
                        + "WHERE u.vps4_user_id = ?;",
                Sql.listOf(this::mapSnapshot), vps4UserId);
    }

    @Override
    public Snapshot getSnapshot(UUID id) {
        return Sql.with(dataSource).exec(selectSnapshotQuery + "WHERE s.id=?",
                Sql.nextOrNull(this::mapSnapshot), id);
    }

    @Override
    public SnapshotWithDetails getSnapshotWithDetails(UUID id) {
        return Sql.with(dataSource).exec(selectSnapshotQuery + "WHERE s.id=?",
                Sql.nextOrNull(this::mapSnapshotWithDetails), id);
    }

    @Override
    public List<Snapshot> getSnapshotsForVm(UUID vmId) {
        return Sql.with(dataSource).exec(selectSnapshotQuery + "WHERE s.vm_id=?",
                Sql.listOf(this::mapSnapshot), vmId);
    }

    private Snapshot mapSnapshot(ResultSet rs) throws SQLException {
        Timestamp modifiedAt = rs.getTimestamp("modified_at");
        return new Snapshot(
                UUID.fromString(rs.getString("id")),
                rs.getLong("project_id"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("name"),
                SnapshotStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                modifiedAt != null ? modifiedAt.toInstant() : null
        );
    }

    private SnapshotWithDetails mapSnapshotWithDetails(ResultSet rs) throws SQLException {
        Timestamp modifiedAt = rs.getTimestamp("modified_at");
        return new SnapshotWithDetails(
                UUID.fromString(rs.getString("id")),
                rs.getString("hfs_image_id"),
                rs.getLong("project_id"),
                rs.getLong("hfs_snapshot_id"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("name"),
                SnapshotStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                modifiedAt != null ? modifiedAt.toInstant() : null
        );
    }
}
