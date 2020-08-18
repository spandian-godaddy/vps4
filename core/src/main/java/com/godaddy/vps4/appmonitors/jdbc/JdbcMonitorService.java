package com.godaddy.vps4.appmonitors.jdbc;

import static java.util.Arrays.stream;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.appmonitors.BackupJobAuditData;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.MonitoringCheckpoint;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.HvBlockingSnapshotsData;
import com.godaddy.vps4.jdbc.Vps4ReportsDataSource;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.Inject;

public class JdbcMonitorService implements MonitorService {

    private final DataSource dataSource;

    private final static String selectVmsByAutomaticSnapshotActionAndDuration =
            "SELECT sna.id, sna.command_id, sna.snapshot_id, action_type.type as action_type, " +
            "action_status.status as action_status, sna.created as action_created_date " +
            "FROM snapshot_action sna " +
            "JOIN action_type ON sna.action_type_id=action_type.type_id " +
            "JOIN action_status ON sna.status_id=action_status.status_id " +
            "JOIN snapshot ON sna.snapshot_id=snapshot.id " +
            "WHERE sna.created < now_utc() " +
            "AND sna.status_id IN ( " +
            "  SELECT status_id FROM action_status WHERE status INCLAUSE " +
            ") AND snapshot.status NOT IN ( " +
            "  SELECT snapshot_status.status_id FROM snapshot_status WHERE snapshot_status.status in (" +
            "'CANCELLED', 'ERROR', 'ERROR_RESCHEDULED', 'LIMIT_RESCHEDULED', 'DESTROYED', 'AGENT_DOWN'" +
            ") " +
            ") AND snapshot.snapshot_type_id = ( " +
            "  SELECT snapshot_type.snapshot_type_id FROM snapshot_type where snapshot_type.snapshot_type = 'AUTOMATIC' " +
            ") " +
            "AND now_utc() - sna.created >= ";

    private final static String orderBySnapshotCreated = "ORDER BY sna.created ASC; ";

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
            "AND st.platform = 'OPENSTACK' ";  // ensure only VPS4 vms are filtered since DED does not have scheduled backups

    @Inject
    public JdbcMonitorService(@Vps4ReportsDataSource DataSource reportsDataSource) {
        this.dataSource = reportsDataSource;
    }

    @Override
    public List<SnapshotActionData> getVmsBySnapshotActions(long thresholdInMinutes, ActionStatus... status) {
        String interval = "INTERVAL '" + thresholdInMinutes + " minutes'  ";
        String[] actionStatuses = stream(status).map(ActionStatus::name).toArray(String[]::new);
        String inClause = "IN ('" + String.join("','", actionStatuses) + "')";
        String selectVmsBySnapshotActionAndDurationWithInClause = selectVmsByAutomaticSnapshotActionAndDuration.replaceFirst("INCLAUSE", inClause);
        String selectDateOrderedVmsBySnapshotActionAndDuration = selectVmsBySnapshotActionAndDurationWithInClause + interval + orderBySnapshotCreated;

        return Sql.with(dataSource)
                .exec(selectDateOrderedVmsBySnapshotActionAndDuration, Sql.listOf(this::mapSnapshotActionData));
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
    public MonitoringCheckpoint getMonitoringCheckpoint(ActionType actionType) {
        return Sql.with(dataSource).exec("SELECT * from monitoring_checkpoint where action_type_id = ?",
                Sql.nextOrNull(this::mapActionCheckpoint),
                actionType.getActionTypeId());
    }

    @Override
    public MonitoringCheckpoint setMonitoringCheckpoint(ActionType actionType) {
        MonitoringCheckpoint checkpoint = getMonitoringCheckpoint(actionType);
        String upsertCheckpointQuery;
        if (checkpoint == null) {
            upsertCheckpointQuery = "INSERT INTO monitoring_checkpoint(action_type_id) VALUES(?) RETURNING *";
        } else {
            upsertCheckpointQuery = "UPDATE monitoring_checkpoint SET checkpoint = now_utc() WHERE action_type_id = ? RETURNING *";
        }

        return Sql.with(dataSource).exec(upsertCheckpointQuery,
                Sql.nextOrNull(this::mapActionCheckpoint),
                actionType.getActionTypeId());
    }

    @Override
    public void deleteMonitoringCheckpoint(ActionType actionType) {
        Sql.with(dataSource).exec("DELETE FROM monitoring_checkpoint WHERE action_type_id = ?", null,
                actionType.getActionTypeId());
    }

    @Override
    public List<MonitoringCheckpoint> getMonitoringCheckpoints() {
        return Sql.with(dataSource).exec("SELECT * FROM monitoring_checkpoint", Sql.listOf(this::mapActionCheckpoint));
    }

    private MonitoringCheckpoint mapActionCheckpoint(ResultSet resultSet) throws SQLException {
        if(resultSet == null) {
            return null;
        }

        MonitoringCheckpoint checkpoint = new MonitoringCheckpoint();
        checkpoint.actionType = ActionType.valueOf(resultSet.getInt("action_type_id"));
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
