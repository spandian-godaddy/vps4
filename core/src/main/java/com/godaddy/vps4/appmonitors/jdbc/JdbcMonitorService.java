package com.godaddy.vps4.appmonitors.jdbc;

import static java.util.Arrays.stream;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.appmonitors.BackupJobAuditData;
import com.godaddy.vps4.appmonitors.Checkpoint;
import com.godaddy.vps4.appmonitors.HvBlockingSnapshotsData;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.ActionCheckpoint;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.jdbc.Vps4ReportsDataSource;
import com.godaddy.vps4.snapshot.SnapshotType;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.Inject;

public class JdbcMonitorService implements MonitorService {
    private final DataSource dataSource;

    private String buildSnapshotActionQuery(String actionStatuses, long thresholdInMinutes, String... filters) {
        return "SELECT sna.id, sna.command_id, sna.snapshot_id, action_type.type as action_type, " +
                "action_status.status as action_status, sna.created as action_created_date " +
                "FROM snapshot_action sna " +
                "JOIN action_type ON sna.action_type_id=action_type.type_id " +
                "JOIN action_status ON sna.status_id=action_status.status_id " +
                "JOIN snapshot ON sna.snapshot_id=snapshot.id " +
                "WHERE sna.status_id IN ( " +
                "  SELECT status_id FROM action_status WHERE status IN ('" + actionStatuses + "') " +
                ") " +
                "AND snapshot.status NOT IN ( " +
                "  SELECT snapshot_status.status_id FROM snapshot_status WHERE snapshot_status.status IN ( " +
                "    'CANCELLED', 'ERROR', 'ERROR_RESCHEDULED', 'LIMIT_RESCHEDULED', 'DESTROYED', 'AGENT_DOWN' " +
                "  ) " +
                ") " +
                "AND now_utc() - sna.created >= INTERVAL '" + thresholdInMinutes + " minutes' " +
                String.join(" ", filters) + " ORDER BY sna.created ASC";
    }

    private final static String selectVmsFilteredByNullBackupJob = "SELECT vm.vm_id, vm.valid_on, * FROM virtual_machine vm " +
            "JOIN vm_action USING (vm_id) " +
            "JOIN virtual_machine_spec vmspec USING (spec_id) " +
            "JOIN server_type st USING (server_type_id) " +
            "JOIN action_status USING (status_id) " +
            "JOIN action_type ON vm_action.action_type_id = action_type.type_id " +
            "WHERE vm.valid_until = 'infinity' " +
            "AND action_status.status = 'COMPLETE' " +
            "AND action_type.type = 'CREATE_VM' " +
            "AND backup_job_id IS NULL " +
            "AND st.platform != 'OVH' ";  // ensure only VPS4 vms are filtered since DED does not have scheduled backups

    @Inject
    public JdbcMonitorService(@Vps4ReportsDataSource DataSource reportsDataSource) {
        this.dataSource = reportsDataSource;
    }

    @Override
    public List<SnapshotActionData> getVmsBySnapshotActions(long thresholdInMinutes, ActionStatus... status) {
        String actionStatuses = stream(status).map(ActionStatus::name).collect(Collectors.joining("','"));

        String query = buildSnapshotActionQuery(actionStatuses, thresholdInMinutes);
        return Sql.with(dataSource).exec(query, Sql.listOf(this::mapSnapshotActionData));
    }

    @Override
    public List<SnapshotActionData> getVmsBySnapshotActions(long thresholdInMinutes,
                                                            SnapshotType type,
                                                            ActionStatus... status) {
        String actionStatuses = stream(status).map(ActionStatus::name).collect(Collectors.joining("','"));
        String typeFilter = "AND snapshot.snapshot_type_id IN ( " +
                "SELECT snapshot_type_id FROM snapshot_type WHERE snapshot_type IN ('" + type.name() + "') " +
                ")";

        String query = buildSnapshotActionQuery(actionStatuses, thresholdInMinutes, typeFilter);
        return Sql.with(dataSource).exec(query, Sql.listOf(this::mapSnapshotActionData));
    }

    private SnapshotActionData mapSnapshotActionData(ResultSet rs) throws SQLException {

        try {
            String actionId = rs.getString("id");
            UUID commandId = rs.getString("command_id") == null ? null : java.util.UUID.fromString(rs.getString("command_id"));
            UUID snapshotId = rs.getString("snapshot_id") == null ? null : java.util.UUID.fromString(rs.getString("snapshot_id"));
            String actionType = ActionType.valueOf(rs.getString("action_type")).name();
            String actionStatus = ActionStatus.valueOf(rs.getString("action_status")).name();
            String createdDate = rs.getString("action_created_date");

            SnapshotActionData snapshotActionData = new SnapshotActionData(actionId, commandId, snapshotId, actionType, actionStatus, createdDate);
            return snapshotActionData;

        } catch (IllegalArgumentException iax) {
            throw new IllegalArgumentException("Could not map response. ", iax);
        }
    }

    @Override
    public List<HvBlockingSnapshotsData> getHvsBlockingSnapshots(long thresholdInHours) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM vm_hypervisor_snapshottracking WHERE now_utc() - created >= INTERVAL '" + thresholdInHours + " hours'",
              Sql.listOf(this::mapHvBlockingSnapshotsData));
    }

    @Override
    public List<BackupJobAuditData> getVmsFilteredByNullBackupJob() {
        return Sql.with(dataSource)
                .exec(selectVmsFilteredByNullBackupJob, Sql.listOf(this::mapVmId));
    }

    @Override
    public ActionCheckpoint getActionCheckpoint(ActionType actionType) {
        return Sql.with(dataSource).exec("SELECT * from action_checkpoint where action_type_id = ?",
                                         Sql.nextOrNull(this::mapActionCheckpoint),
                                         actionType.getActionTypeId());
    }

    @Override
    public ActionCheckpoint setActionCheckpoint(ActionType actionType) {
        ActionCheckpoint checkpoint = getActionCheckpoint(actionType);
        String upsertCheckpointQuery;
        if (checkpoint == null) {
            upsertCheckpointQuery = "INSERT INTO action_checkpoint(action_type_id) VALUES(?) RETURNING *";
        } else {
            upsertCheckpointQuery = "UPDATE action_checkpoint SET checkpoint = now_utc() WHERE action_type_id = ? RETURNING *";
        }

        return Sql.with(dataSource).exec(upsertCheckpointQuery,
                                         Sql.nextOrNull(this::mapActionCheckpoint),
                                         actionType.getActionTypeId());
    }

    @Override
    public void deleteActionCheckpoint(ActionType actionType) {
        Sql.with(dataSource).exec("DELETE FROM action_checkpoint WHERE action_type_id = ?", null,
                                  actionType.getActionTypeId());
    }

    @Override
    public List<ActionCheckpoint> getActionCheckpoints() {
        return Sql.with(dataSource).exec("SELECT * FROM action_checkpoint", Sql.listOf(this::mapActionCheckpoint));
    }

    @Override
    public Checkpoint getCheckpoint(Checkpoint.Name name) {
        return Sql.with(dataSource).exec("SELECT * from checkpoint where name = ?",
                                         Sql.nextOrNull(this::mapCheckpoint),
                                         name.toString());
    }

    @Override
    public Checkpoint setCheckpoint(Checkpoint.Name name) {
        Checkpoint checkpoint = getCheckpoint(name);
        String upsertCheckpointQuery;
        if (checkpoint == null) {
            upsertCheckpointQuery = "INSERT INTO checkpoint(name) VALUES(?) RETURNING *";
        } else {
            upsertCheckpointQuery = "UPDATE checkpoint SET checkpoint = now_utc() WHERE name = ? RETURNING *";
        }

        return Sql.with(dataSource).exec(upsertCheckpointQuery, Sql.nextOrNull(this::mapCheckpoint), name.toString());
    }

    @Override
    public void deleteCheckpoint(Checkpoint.Name name) {
        Sql.with(dataSource).exec("DELETE FROM checkpoint WHERE name = ?", null, name.toString());
    }

    @Override
    public List<Checkpoint> getCheckpoints() {
        return Sql.with(dataSource).exec("SELECT * FROM checkpoint", Sql.listOf(this::mapCheckpoint));
    }

    private ActionCheckpoint mapActionCheckpoint(ResultSet resultSet) throws SQLException {
        if (resultSet == null) {
            return null;
        }

        ActionCheckpoint checkpoint = new ActionCheckpoint();
        checkpoint.actionType = ActionType.valueOf(resultSet.getInt("action_type_id"));
        checkpoint.checkpoint = resultSet.getTimestamp("checkpoint", TimestampUtils.utcCalendar).toInstant();

        return checkpoint;
    }

    private Checkpoint mapCheckpoint(ResultSet resultSet) throws SQLException {
        if (resultSet == null) {
            return null;
        }

        Checkpoint checkpoint = new Checkpoint();
        checkpoint.name = Checkpoint.Name.valueOf(resultSet.getString("name"));
        checkpoint.checkpoint = resultSet.getTimestamp("checkpoint", TimestampUtils.utcCalendar).toInstant();

        return checkpoint;
    }

    private HvBlockingSnapshotsData mapHvBlockingSnapshotsData(ResultSet rs) throws SQLException {
        return new HvBlockingSnapshotsData(
                rs.getString("hypervisor"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant()
        );
    }

    private BackupJobAuditData mapVmId(ResultSet rs) throws SQLException {
        try {
            UUID vmId = UUID.fromString(rs.getString("vm_id"));
            Instant validOn = rs.getTimestamp("valid_on", TimestampUtils.utcCalendar).toInstant();

            BackupJobAuditData auditData = new BackupJobAuditData(vmId, validOn);
            return auditData;
        } catch (IllegalArgumentException iax) {
            throw new IllegalArgumentException("Could not map response. ", iax);
        }
    }
}
