package com.godaddy.vps4.snapshot.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.snapshot.Snapshot;
import com.godaddy.vps4.snapshot.SnapshotService;
import com.godaddy.vps4.snapshot.SnapshotStatus;

public class JdbcSnapshotService implements SnapshotService {
    private static final long OPEN_SLOTS_PER_CREDIT = 1;
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
    public boolean isOverQuota(UUID orionGuid) {
        return !(hasOpenSlots(orionGuid) || allSlotsFilledOnlyByLiveSnapshots(orionGuid));
    }

    private boolean hasOpenSlots(UUID orionGuid) {
        // check if number of snapshots (not in error and not destroyed) linked to the credit is over the number
        // of open slots available. Right now the number of open slots is hard coded to 1 but this might change in
        // the future as HEG and MT get on-boarded.
        return Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM SNAPSHOT s JOIN SNAPSHOT_STATUS ss ON s.status = ss.status_id "
                        + "JOIN VIRTUAL_MACHINE v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? AND ss.status NOT IN ('ERROR', 'DESTROYED');",
                this::hasOpenSlotsMapper, orionGuid);
    }

    private boolean hasOpenSlotsMapper(ResultSet rs) throws SQLException {
        return rs.next() && rs.getLong("count") < OPEN_SLOTS_PER_CREDIT;
    }

    private boolean allSlotsFilledOnlyByLiveSnapshots(UUID orionGuid) {
        List<StatusCount> statusCounts = Sql.with(dataSource).exec(
                "SELECT ss.status, COUNT(*) FROM SNAPSHOT s JOIN snapshot_status ss ON s.status = ss.status_id "
                        + "JOIN VIRTUAL_MACHINE v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? "
                        + "AND ss.status NOT IN ('ERROR', 'DESTROYED') "
                        + "GROUP BY ss.status;",
                Sql.listOf(this::mapStatusCount), orionGuid);
        long numInNew = getCountForStatus(statusCounts, SnapshotStatus.NEW);
        long numInProgress = getCountForStatus(statusCounts, SnapshotStatus.IN_PROGRESS);
        long numLive = getCountForStatus(statusCounts, SnapshotStatus.LIVE);
        long numDeprecating = getCountForStatus(statusCounts, SnapshotStatus.DEPRECATING);
        long numDeprecated = getCountForStatus(statusCounts, SnapshotStatus.DEPRECATED);

        // If we have all the available slots filled up by snapshots that are 'LIVE', then we can deprecate the oldest
        // Right now the number of open slots is hard coded to 1 but this might change in
        // the future as HEG and MT get on-boarded.
        return (numLive == OPEN_SLOTS_PER_CREDIT)
                && (numInNew == 0)
                && (numInProgress == 0)
                && (numDeprecating == 0)
                && (numDeprecated == 0);
    }

    private long getCountForStatus(List<StatusCount> statusCounts, SnapshotStatus status) {
        Stream<StatusCount> statusCountStream = statusCounts.stream().filter(sc -> sc.status.equals(status));
        return statusCountStream.findFirst().orElse(new StatusCount(status, 0)).count;
    }

    private class StatusCount {
        final SnapshotStatus status;
        final long count;

        private StatusCount(SnapshotStatus status, long count) {
            this.status = status;
            this.count = count;
        }
    }

    private StatusCount mapStatusCount(ResultSet rs) throws SQLException {
        return new StatusCount(
                SnapshotStatus.valueOf(rs.getString("status")),
                rs.getLong("count"));
    }

    private boolean shouldDeprecateSnapshot(UUID orionGuid) {
        // we should be deprecating a snapshot only if the number of LIVE snapshot is
        // equal to the max number of slots for the account (orionGuid)
        return Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM SNAPSHOT s JOIN SNAPSHOT_STATUS ss ON s.status = ss.status_id "
                        + "JOIN VIRTUAL_MACHINE v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? AND ss.status IN ('LIVE');",
                this::shouldDeprecateMapper, orionGuid);
    }

    private boolean shouldDeprecateMapper(ResultSet rs) throws SQLException {
        return rs.next() && rs.getLong("count") == OPEN_SLOTS_PER_CREDIT;
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
        updateSnapshotStatus(snapshotId, SnapshotStatus.IN_PROGRESS);
    }

    @Override
    public void markSnapshotLive(UUID snapshotId) {
        updateSnapshotStatus(snapshotId, SnapshotStatus.LIVE);
    }

    @Override
    public void markSnapshotErrored(UUID snapshotId) {
        updateSnapshotStatus(snapshotId, SnapshotStatus.ERROR);
    }

    @Override
    public void markSnapshotDestroyed(UUID snapshotId) {
        updateSnapshotStatus(snapshotId, SnapshotStatus.DESTROYED);
    }

    @Override
    public void markSnapshotAsDeprecated(UUID snapshotId) {
        updateSnapshotStatus(snapshotId, SnapshotStatus.DEPRECATED);
    }

    private void markSnapshotForDeprecation(UUID snapshotId) {
        updateSnapshotStatus(snapshotId, SnapshotStatus.DEPRECATING);
    }

    private UUID getOldestLiveSnapshot(UUID orionGuid) {
        return Sql.with(dataSource).exec(
                "SELECT s.id FROM SNAPSHOT s JOIN SNAPSHOT_STATUS ss ON s.status = ss.status_id "
                        + "JOIN VIRTUAL_MACHINE v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? AND ss.status IN ('LIVE') "
                        + "ORDER BY s.created_at LIMIT 1;",
                Sql.nextOrNull(rs -> UUID.fromString(rs.getString("id"))), orionGuid);
    }

    @Override
    public void reverseSnapshotDeprecation(UUID snapshotId) {
        // Reversing deprecation just sets a snapshot back to live
        markSnapshotLive(snapshotId);
    }

    @Override
    public void updateSnapshotStatus(UUID snapshotId, SnapshotStatus status) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=NOW(), status=? "
                + " WHERE id=?", null, status.getSnapshotStatusId(), snapshotId);
    }

    @Override
    public UUID markOldestSnapshotForDeprecation(UUID orionGuid) {
        if (shouldDeprecateSnapshot(orionGuid)) {
            UUID snapshotId = getOldestLiveSnapshot(orionGuid);
            if (snapshotId != null) {
                markSnapshotForDeprecation(snapshotId);
                return snapshotId;
            }
        }

        return null;
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
    public List<Snapshot> getSnapshotsByOrionGuid(UUID orionGuid) {
        return Sql.with(dataSource).exec(selectSnapshotQuery
                        + "JOIN virtual_machine v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ?;",
                Sql.listOf(this::mapSnapshot), orionGuid);
    }

    @Override
    public Snapshot getSnapshot(UUID id) {
        return Sql.with(dataSource).exec(selectSnapshotQuery + "WHERE s.id=?",
                Sql.nextOrNull(this::mapSnapshot), id);
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
                modifiedAt != null ? modifiedAt.toInstant() : null,
                rs.getString("hfs_image_id"),
                rs.getLong("hfs_snapshot_id")
        );
    }

}
