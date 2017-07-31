package com.godaddy.vps4.snapshot.jdbc;

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
    public UUID createSnapshot(long projectId, UUID vmId, String name) {
        UUID snapshotId = UUID.randomUUID();
        Sql.with(dataSource).exec("INSERT INTO snapshot (id, name, project_id, vm_id) "
                        + "VALUES (?, ?, ?, ?);", null, snapshotId, name, projectId, vmId);
        return snapshotId;
    }

    @Override
    public boolean isOverQuota(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM SNAPSHOT JOIN snapshot_status ON snapshot.status = snapshot_status.status_id "
                + " WHERE vm_id = ? AND snapshot_status.status NOT IN ('ERROR', 'DESTROYED')",
                this::isOverQuotaMapper, vmId);
    }

    private boolean isOverQuotaMapper(ResultSet rs) throws SQLException {
        return rs.next() && rs.getLong("count") > 0;
    }

    @Override
    public void updateHfsSnapshotId(UUID snapshotId, long hfsSnapshotId) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=NOW(), hfs_snapshot_id=? "
                + " WHERE id=?", null, hfsSnapshotId, snapshotId);
    }

    @Override
    public void updateHfsImageId(UUID snapshotId, String hfsImageId) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=NOW(), hfs_image_id=? "
                + " WHERE id=?", null, hfsImageId, snapshotId);
    }

    @Override
    public void markSnapshotInProgress(UUID snapshotId) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=NOW(), status=? "
                + " WHERE id=?", null, SnapshotStatus.IN_PROGRESS.getSnapshotStatusId(), snapshotId);
    }

    @Override
    public void markSnapshotComplete(UUID snapshotId) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=NOW(), status=? "
                + " WHERE id=?", null, SnapshotStatus.COMPLETE.getSnapshotStatusId(), snapshotId);
    }

    @Override
    public void markSnapshotErrored(UUID snapshotId) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=NOW(), status=? "
                + " WHERE id=?", null, SnapshotStatus.ERROR.getSnapshotStatusId(), snapshotId);
    }

    @Override
    public void markSnapshotDestroyed(UUID snapshotId) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=NOW(), status=? "
                + " WHERE id=?", null, SnapshotStatus.DESTROYED.getSnapshotStatusId(), snapshotId);
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
    public List<SnapshotWithDetails> getSnapshotsByOrionGuid(UUID orionGuid) {
        return Sql.with(dataSource).exec(selectSnapshotQuery
                        + "JOIN virtual_machine v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ?;",
                Sql.listOf(this::mapSnapshotWithDetails), orionGuid);
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
