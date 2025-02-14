package com.godaddy.vps4.snapshot.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

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
    private final String selectSnapshotQuery = "SELECT s.id, s.hfs_image_id, s.project_id, "
            + "s.hfs_snapshot_id, s.vm_id, s.name, ss.status, s.created_at, s.modified_at, st.snapshot_type "
            + "FROM snapshot s JOIN snapshot_status ss ON s.status = ss.status_id "
            + "JOIN snapshot_type st USING(snapshot_type_id) ";

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
    public int totalFilledSlots(UUID orionGuid, SnapshotType snapshotType) {
        // check the number of snapshots (not in error, destroyed, cancelled, error_rescheduled, limit_rescheduled, or
        // agent_down) linked to the credit
        return Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM snapshot s JOIN snapshot_status ss ON s.status = ss.status_id "
                        + "JOIN virtual_machine v ON s.vm_id = v.vm_id "
                        + "WHERE v.orion_guid = ? AND ss.status IN ("
                        + "'NEW', 'IN_PROGRESS', 'LIVE', 'DEPRECATING', 'DEPRECATED'"
                        + ") AND s.snapshot_type_id = ?;",
                Sql.nextOrNull(rs -> rs.getInt("count")), orionGuid, snapshotType.getSnapshotTypeId());
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
                "SELECT COUNT(*) FROM snapshot s JOIN snapshot_status ss ON s.status = ss.status_id "
                        + "JOIN virtual_machine v ON s.vm_id = v.vm_id "
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

    @Override
    public void updateSnapshotStatus(UUID snapshotId, SnapshotStatus status) {
        Sql.with(dataSource).exec("UPDATE snapshot SET modified_at=now_utc(), status=? "
                + " WHERE id=?", null, status.getSnapshotStatusId(), snapshotId);
    }

    @Override
    public Snapshot getOldestLiveSnapshot(UUID orionGuid, SnapshotType type) {
        return Sql.with(dataSource).exec(selectSnapshotQuery
                                                 + "JOIN virtual_machine v ON s.vm_id = v.vm_id "
                                                 + "WHERE v.orion_guid = ? AND ss.status IN ('LIVE') "
                                                 + "AND st.snapshot_type = ? "
                                                 + "ORDER BY s.created_at LIMIT 1",
                                         Sql.nextOrNull(this::mapSnapshot), orionGuid, type.name());
    }

    @Override
    public UUID markOldestSnapshotForDeprecation(UUID orionGuid, SnapshotType snapshotType) {
        if (shouldDeprecateSnapshot(orionGuid, snapshotType)) {
            Snapshot snapshot = getOldestLiveSnapshot(orionGuid, snapshotType);
            if (snapshot != null) {
                updateSnapshotStatus(snapshot.id, SnapshotStatus.DEPRECATING);
                return snapshot.id;
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
        return Sql.with(dataSource).exec(selectSnapshotQuery + "WHERE s.vm_id=? ORDER BY modified_at DESC",
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
