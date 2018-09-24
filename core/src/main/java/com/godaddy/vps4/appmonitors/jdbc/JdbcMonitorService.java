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
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.jdbc.Vps4ReportsDataSource;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.Inject;

public class JdbcMonitorService implements MonitorService {

    private final DataSource reportsDataSource;

    private final static String selectVmsByActionAndDuration =
            "SELECT vma.id, vma.command_id, vma.vm_id, action_type.type as action_type " +
            "FROM vm_action vma " +
            "JOIN action_type ON vma.action_type_id = action_type.type_id " +
            "WHERE vma.created < now_utc() " +
            "AND vma.action_type_id = ( " +
            "  SELECT type_id FROM action_type WHERE type = ? " +
            ") " +
            "AND vma.status_id = ( " +
            "  SELECT status_id FROM action_status WHERE status = ? " +
            ") " +
            "AND now_utc() - vma.created >= ";

    private final static String orderby = "ORDER BY vma.created ASC; ";

    private final static String selectVmsBySnapshotActionAndDuration =
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
            "  SELECT snapshot_status.status_id FROM snapshot_status WHERE snapshot_status.status = 'CANCELLED' " +
            ") " +
            "AND now_utc() - sna.created >= ";

    private final static String orderBySnapshotCreated = "ORDER BY sna.created ASC; ";

    private final static String selectVmsByActionStatusAndDuration = "SELECT vma.id, vma.command_id, vma.vm_id, action_type.type " +
            "FROM vm_action vma, action_type " +
            "WHERE vma.created < now_utc() " +
            "AND vma.action_type_id = action_type.type_id " +
            "AND vma.status_id = ( " +
            "  SELECT status_id FROM action_status WHERE status = ? " +
            ") " +
            "AND now_utc() - vma.created >= ";

    private final static String selectVmsFilteredByNullBackupJob = "SELECT vm.vm_id, vm.valid_on FROM virtual_machine vm " +
            "JOIN vm_action USING (vm_id) " +
            "JOIN virtual_machine_spec vmspec USING (spec_id) " +
            "JOIN server_type st USING (server_type_id) " +
            "JOIN action_status ON action_status.status_id = vm_action.status_id AND status = 'COMPLETE' " +
            "JOIN action_type ON action_type.type_id = vm_action.action_type_id AND type = 'CREATE_VM'  " +
            "WHERE vm.valid_until = 'infinity' " +
            "AND backup_job_id IS NULL " +
            "AND st.platform = 'OPENSTACK' ";  // ensure only VPS4 vms are filtered since DED does not have scheduled backups

    @Inject
    public JdbcMonitorService(@Vps4ReportsDataSource DataSource reportsDataSource) {
        this.reportsDataSource = reportsDataSource;
    }

    @Override
    public List<VmActionData> getVmsByActions(long thresholdInMinutes, ActionType type, ActionStatus status) {
        String interval = "INTERVAL '" + thresholdInMinutes + " minutes'";
        String selectDateOrderedVmsByActionAndDuration = selectVmsByActionAndDuration + interval + orderby;
        return Sql.with(reportsDataSource)
                .exec(selectDateOrderedVmsByActionAndDuration, Sql.listOf(this::mapVmActionData), type.name(), status.name());
    }

    private VmActionData mapVmActionData(ResultSet rs) throws SQLException {

        try {
            String actionId = rs.getString("id");
            UUID commandId = rs.getString("command_id") == null ? null : java.util.UUID.fromString(rs.getString("command_id"));
            UUID vmId = rs.getString("vm_id") == null ? null : java.util.UUID.fromString(rs.getString("vm_id"));

            VmActionData vmActionData = new VmActionData(actionId, commandId, vmId);
            return vmActionData;
        }catch (IllegalArgumentException iax) {
            throw new IllegalArgumentException("Could not map response. ", iax);
        }
    }

    @Override
    public List<SnapshotActionData> getVmsBySnapshotActions(long thresholdInMinutes, ActionStatus... status) {
        String interval = "INTERVAL '" + thresholdInMinutes + " minutes'  ";
        String[] actionStatuses = stream(status).map(ActionStatus::name).toArray(String[]::new);
        String inClause = "IN ('" + String.join("','", actionStatuses) + "')";
        String selectVmsBySnapshotActionAndDurationWithInClause = selectVmsBySnapshotActionAndDuration.replaceFirst("INCLAUSE", inClause);
        String selectDateOrderedVmsBySnapshotActionAndDuration = selectVmsBySnapshotActionAndDurationWithInClause + interval + orderBySnapshotCreated;
        return Sql.with(reportsDataSource)
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
    public List<VmActionData> getVmsByActionStatus(long thresholdInMinutes, ActionStatus status) {
        String interval = "INTERVAL '" + thresholdInMinutes + " minutes'  ";
        String selectDateOrderedVmsByActionStatusAndDuration = selectVmsByActionStatusAndDuration + interval + orderby;
        return Sql.with(reportsDataSource)
                .exec(selectDateOrderedVmsByActionStatusAndDuration, Sql.listOf(this::mapVmActionDataWithActionType), status.name());
    }

    private VmActionData mapVmActionDataWithActionType(ResultSet rs) throws SQLException {

        try {
            String actionId = rs.getString("id");
            UUID commandId = rs.getString("command_id") == null ? null : java.util.UUID.fromString(rs.getString("command_id"));
            UUID vmId = rs.getString("vm_id") == null ? null : java.util.UUID.fromString(rs.getString("vm_id"));
            String actionType = rs.getString("type") == null ? null : ActionType.valueOf(rs.getString("type")).name();

            VmActionData vmActionData = new VmActionData(actionId, commandId, vmId, actionType);
            return vmActionData;
        } catch (IllegalArgumentException iax) {
            throw new IllegalArgumentException("Could not map response. ", iax);
        }
    }

    @Override
    public List<BackupJobAuditData> getVmsFilteredByNullBackupJob() {
        return Sql.with(reportsDataSource)
                .exec(selectVmsFilteredByNullBackupJob, Sql.listOf(this::mapVmId));
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
