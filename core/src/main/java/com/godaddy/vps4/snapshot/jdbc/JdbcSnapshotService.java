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
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.util.TimestampUtils;

public class JdbcSnapshotService implements SnapshotService {
    private static final long OPEN_SLOTS_PER_CREDIT = 1;
    private final DataSource dataSource;
    private String selectSnapshotQuery = "SELECT s.id, s.hfs_image_id, s.project_id, "
            + "s.hfs_snapshot_id, s.vm_id, s.name, ss.status, s.created_at, s.modified_at, st.snapshot_type "
            + "FROM snapshot s JOIN snapshot_status ss ON s.status = ss.status_id "
            + "JOIN snapshot_type st  USING(snapshot_type_id) ";
    private String orderByCompletedDateDesc = "ORDER BY modified_at DESC ";

    @Inject
    public JdbcSnapshotService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public UUID createSnapshot(long projectId, UUID vmId, String name, SnapshotType type) {
        UUID snapshotId = UUID.randomUUID();
        int snapshotTypeId = type.getSnapshotTypeId();
        Sql.with(dataSource).exec("INSERT INTO snapshot (id, name, project_id, vm_id, snapshot_type_id) "
                        + "VALUES (?, ?, ?, ?, ?);", null, snapshotId, name, projectId, vmId, snapshotTypeId);
        return snapshotId;
    }

    @Override
    public void renameSnapshot(UUID snapshotId, String name) {
        Sql.with(dataSource).exec("UPDATE snapshot SET name = ? WHERE id = ?;",
                null, name, snapshotId);
    }

    @Override
    public boolean isOverQuota(UUID orionGuid, SnapshotType snapshotType) {
        // If there is not an open slot or there is a snapshot that isn't live then it's over quota
        return !(hasOpenSlots(orionGuid, snapshotType) ||
                allSlotsFilledOnlyByLiveSnapshots(orionGuid, snapshotType));
    }

    private boolean hasOpenSlots(UUID orionGuid, SnapshotType snapshotType) {
        // check if number of snapshots (not in error, destroyed, cancelled, error_rescheduled, limit_rescheduled, or
        // agent_down) linked to the credit is over the number of open slots available. Right now the number of open
        // slots is hard coded to 1 but this might change in the future as HEG and MT get on-boarded.
        return Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM SNAPSHOT s JOIN SNAPSHOT_STATUS ss ON s.status = ss.status_id "
                        + "JOIN VIRTUAL_MACHINE v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? AND ss.status NOT IN ("
                        + "'ERROR', 'DESTROYED', 'CANCELLED', 'ERROR_RESCHEDULED', 'LIMIT_RESCHEDULED', 'AGENT_DOWN'"
                        + ") AND s.snapshot_type_id = ?;",
                this::hasOpenSlotsMapper, orionGuid, snapshotType.getSnapshotTypeId());
    }

    private boolean hasOpenSlotsMapper(ResultSet rs) throws SQLException {
        return rs.next() && rs.getLong("count") < OPEN_SLOTS_PER_CREDIT;
    }

    private boolean allSlotsFilledOnlyByLiveSnapshots(UUID orionGuid, SnapshotType snapshotType) {
        List<StatusCount> statusCounts = Sql.with(dataSource).exec(
                "SELECT ss.status, COUNT(*) FROM SNAPSHOT s JOIN snapshot_status ss ON s.status = ss.status_id "
                        + "JOIN VIRTUAL_MACHINE v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? "
                        + "AND ss.status NOT IN ("
                        + "'ERROR', 'DESTROYED', 'CANCELLED', 'ERROR_RESCHEDULED', 'LIMIT_RESCHEDULED', 'AGENT_DOWN'"
                        + ") AND snapshot_type_id = ?"
                        + "GROUP BY ss.status;",
                Sql.listOf(this::mapStatusCount), orionGuid, snapshotType.getSnapshotTypeId());
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

    @Override
    public boolean hasSnapshotInProgress(UUID orionGuid) {
        int numOfRows = Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM snapshot s JOIN snapshot_status ss ON s.status = ss.status_id "
                        + "JOIN virtual_machine v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? "
                        + "AND ss.status NOT IN ("
                        + "'LIVE', 'ERROR', 'DESTROYED', 'CANCELLED', 'ERROR_RESCHEDULED', 'LIMIT_RESCHEDULED', 'AGENT_DOWN'"
                        + ");",
                Sql.nextOrNull(this::mapCountRows), orionGuid);

        // No 2 backups for the same vm should ever be in progress at the same time
        return numOfRows > 0;
    }

    @Override
    public int totalSnapshotsInProgress() {
        return Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM snapshot s JOIN snapshot_status ss ON s.status = ss.status_id "
                        + "WHERE ss.status = 'IN_PROGRESS';",
                Sql.nextOrNull(this::mapCountRows));
    }

    private int mapCountRows(ResultSet rs) throws SQLException {
        return rs.getInt("count");
    }

    private boolean shouldDeprecateSnapshot(UUID orionGuid, SnapshotType snapshotType) {
        // we should be deprecating a snapshot only if the number of LIVE snapshot is
        // equal to the max number of slots for the account (orionGuid)
        return Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM SNAPSHOT s JOIN SNAPSHOT_STATUS ss ON s.status = ss.status_id "
                        + "JOIN VIRTUAL_MACHINE v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? AND ss.status IN ('LIVE') "
                        + "AND s.snapshot_type_id = ?;",
                this::shouldDeprecateMapper, orionGuid, snapshotType.getSnapshotTypeId());
    }

    private boolean shouldDeprecateMapper(ResultSet rs) throws SQLException {
        return rs.next() && rs.getLong("count") == OPEN_SLOTS_PER_CREDIT;
    }

    @Override
    public void updateHfsSnapshotId(UUID snapshotId, long hfsSnapshotId) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=now_utc(), hfs_snapshot_id=? "
                + " WHERE id=?", null, hfsSnapshotId, snapshotId);
    }

    @Override
    public void updateHfsImageId(UUID snapshotId, String hfsImageId) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=now_utc(), hfs_image_id=? "
                + " WHERE id=?", null, hfsImageId, snapshotId);
    }

    private UUID getOldestLiveSnapshot(UUID orionGuid, SnapshotType type) {
        return Sql.with(dataSource).exec(
                "SELECT s.id FROM SNAPSHOT s JOIN SNAPSHOT_STATUS ss ON s.status = ss.status_id "
                        + "JOIN VIRTUAL_MACHINE v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? AND ss.status IN ('LIVE') "
                        + "AND s.snapshot_type_id = ? "
                        + "ORDER BY s.created_at LIMIT 1;",
                Sql.nextOrNull(rs -> UUID.fromString(rs.getString("id"))), orionGuid, type.getSnapshotTypeId());
    }

    @Override
    public void updateSnapshotStatus(UUID snapshotId, SnapshotStatus status) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=now_utc(), status=? "
                + " WHERE id=?", null, status.getSnapshotStatusId(), snapshotId);
    }

    @Override
    public UUID markOldestSnapshotForDeprecation(UUID orionGuid, SnapshotType snapshotType) {
        if (shouldDeprecateSnapshot(orionGuid, snapshotType)) {
            UUID snapshotId = getOldestLiveSnapshot(orionGuid, snapshotType);
            if (snapshotId != null) {
                updateSnapshotStatus(snapshotId, SnapshotStatus.DEPRECATING);
                return snapshotId;
            }
        }

        return null;
    }

    @Override
    public Snapshot getSnapshot(UUID id) {
        return Sql.with(dataSource).exec(selectSnapshotQuery + "WHERE s.id=?",
                Sql.nextOrNull(this::mapSnapshot), id);
    }

    @Override
    public List<Snapshot> getSnapshotsForVm(UUID vmId) {
        return Sql.with(dataSource).exec(selectSnapshotQuery + "WHERE s.vm_id=? " + orderByCompletedDateDesc,
                Sql.listOf(this::mapSnapshot), vmId);
    }

    private Snapshot mapSnapshot(ResultSet rs) throws SQLException {
        Timestamp modifiedAt = rs.getTimestamp("modified_at", TimestampUtils.utcCalendar);
        return new Snapshot(
                UUID.fromString(rs.getString("id")),
                rs.getLong("project_id"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("name"),
                SnapshotStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at", TimestampUtils.utcCalendar).toInstant(),
                modifiedAt != null ? modifiedAt.toInstant() : null,
                rs.getString("hfs_image_id"),
                rs.getLong("hfs_snapshot_id"),
                SnapshotType.valueOf(rs.getString("snapshot_type"))
        );
    }

    private UUID mapSnapshotIds(ResultSet rs) throws SQLException {
        return UUID.fromString(rs.getString("id"));
    }

    @Override
    public void cancelErroredSnapshots(UUID orionGuid, SnapshotType snapshotType){
        List<UUID> ids = Sql.with(dataSource).exec(selectSnapshotQuery
                        + " JOIN virtual_machine v ON s.vm_id = v.vm_id"
                        + " WHERE v.orion_guid = ?"
                        + " AND s.snapshot_type_id = ?"
                        + " AND s.status IN (?, ?, ?, ?);",
                Sql.listOf(this::mapSnapshotIds), orionGuid, snapshotType.getSnapshotTypeId(),
                SnapshotStatus.ERROR.getSnapshotStatusId(),
                SnapshotStatus.ERROR_RESCHEDULED.getSnapshotStatusId(),
                SnapshotStatus.LIMIT_RESCHEDULED.getSnapshotStatusId(),
                SnapshotStatus.AGENT_DOWN.getSnapshotStatusId());
        markSnapshotCancelled(ids);
    }

    private void markSnapshotCancelled(List<UUID> snapshotIds){
        if(snapshotIds.isEmpty()){
            return;
        }

        // transform [uuid, uuid] to ('uuid', 'uuid')
        String ids = snapshotIds.toString().replace("[","('");
        ids = ids.replace("]", "')");
        ids = ids.replace(", ", "', '");

        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=now_utc(), status=? "
                + " WHERE id IN " + ids, null, SnapshotStatus.CANCELLED.getSnapshotStatusId());
    }

    @Override
    public int failedBackupsSinceSuccess(UUID vmId, SnapshotType snapshotType){
        // count the rows of ERRORed, ERROR_RESCHEDULED, and CANCELLED backups since the last successful backup
        // 01/02/03 is an arbitrary time earlier than any backup recorded in this system.  It is used for the case when
        // there are no 'LIVE' backups recorded for the vm yet.
        return Sql.with(dataSource).exec("SELECT COUNT(id) AS numOfIds " +
                "FROM snapshot " +
                "JOIN snapshot_status on snapshot.status = snapshot_status.status_id " +
                "WHERE vm_id = ? " +
                "AND snapshot_status.status in ('ERROR', 'CANCELLED', 'ERROR_RESCHEDULED') " +
                "AND snapshot_type_id = ?" +
                "AND created_at > " +
                    "(GREATEST((SELECT MAX(created_at) AS latestLive " +
                      "FROM snapshot " +
                      "JOIN snapshot_status on snapshot.status = snapshot_status.status_id " +
                      "WHERE vm_id = ? " +
                      "AND snapshot_status.status = 'LIVE' " +
                      "AND snapshot_type_id = ?), '01/02/03'::timestamp));",
                Sql.nextOrNull(rs -> rs.getInt("numOfIds")),
                vmId, snapshotType.getSnapshotTypeId(), vmId, snapshotType.getSnapshotTypeId());
    }

    @Override
    public UUID getVmIdWithInProgressSnapshotOnHv(String hypervisorHostname) {
        return Sql.with(dataSource).exec(
                "SELECT vm_id FROM vm_hypervisor_snapshottracking WHERE hypervisor = ?;",
                Sql.nextOrNull(rs -> UUID.fromString(rs.getString("vm_id"))), hypervisorHostname);
    }

    @Override
    public void saveVmHvForSnapshotTracking(UUID vmId, String hypervisorHostname) {
        Sql.with(dataSource).exec("INSERT INTO vm_hypervisor_snapshottracking (vm_id, hypervisor) "
                                  + "VALUES (?, ?);", null, vmId, hypervisorHostname);
    }

    @Override
    public void deleteVmHvForSnapshotTracking(UUID vmId) {
        Sql.with(dataSource).exec("DELETE FROM vm_hypervisor_snapshottracking WHERE vm_id = ?;",null, vmId);
    }
}
