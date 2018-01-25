package com.godaddy.vps4.appmonitors.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.appmonitors.SnapshotActionData;
import com.godaddy.vps4.appmonitors.VmActionData;
import com.godaddy.vps4.jdbc.Vps4ReportsDataSource;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.Inject;

public class JdbcMonitorService implements MonitorService {

    private final DataSource reportsDataSource;

    private final static String selectVmsByActionAndDuration = "SELECT vma.id, vma.command_id, vma.vm_id " +
            "FROM vm_action vma " +
            "WHERE vma.created < now_utc() " +
            "AND vma.action_type_id = ( " +
            "  SELECT type_id FROM action_type WHERE type = ? " +
            ") " +
            "AND vma.status_id = ( " +
            "  SELECT status_id FROM action_status WHERE status = ? " +
            ") " +
            "AND now_utc() - vma.created >= ";

    private final static String orderby = "ORDER BY vma.created ASC; ";

    private final static String selectVmsBySnapshotActionAndDuration = "SELECT sna.id, sna.command_id, sna.snapshot_id " +
            "FROM snapshot_action sna " +
            "WHERE sna.created < now_utc() " +
            "AND sna.action_type_id = ( " +
            "  SELECT type_id FROM action_type WHERE type = ? " +
            ") " +
            "AND sna.status_id = ( " +
            "  SELECT status_id FROM action_status WHERE status = ? " +
            ") " +
            "AND now_utc() - sna.created >= ";

    private final static String orderBySnapshotCreated = "ORDER BY sna.created ASC; ";

    private final static String selectByActionStatusAndDuration = "SELECT vma.id, vma.command_id, vma.vm_id, action_type.type " +
            "FROM vm_action vma, action_type " +
            "WHERE vma.created < now_utc() " +
            "AND vma.action_type_id = action_type.type_id " +
            "AND vma.status_id = ( " +
            "  SELECT status_id FROM action_status WHERE status = ? " +
            ") " +
            "AND now_utc() - vma.created >= ";

    @Inject
    public JdbcMonitorService(@Vps4ReportsDataSource DataSource reportsDataSource) {
        this.reportsDataSource = reportsDataSource;
    }

    @Override
    public List<VmActionData> getVmsByActions(ActionType type, ActionStatus status, long thresholdInMinutes) {
        String interval = "INTERVAL '" + thresholdInMinutes + " minutes'";
        String selectDateOrderedVmsByActionAndDuration = selectVmsByActionAndDuration + interval + orderby;
        return Sql.with(reportsDataSource)
                .exec(selectDateOrderedVmsByActionAndDuration, Sql.listOf(this::mapVmActionData), type.name(), status.name());
    }

    private VmActionData mapVmActionData(ResultSet rs) throws SQLException {

        String actionId = rs.getString("id");
        UUID commandId = java.util.UUID.fromString(rs.getString("command_id"));
        UUID vmId = java.util.UUID.fromString(rs.getString("vm_id"));

        VmActionData vmActionData = new VmActionData(actionId, commandId, vmId);
        return vmActionData;
    }

    @Override
    public List<SnapshotActionData> getVmsBySnapshotActions(ActionType type, ActionStatus status, long thresholdInMinutes) {
        String interval = "INTERVAL '" + thresholdInMinutes + " minutes'  ";
        String selectDateOrderedVmsBySnapshotActionAndDuration = selectVmsBySnapshotActionAndDuration + interval + orderBySnapshotCreated;
        return Sql.with(reportsDataSource)
                .exec(selectDateOrderedVmsBySnapshotActionAndDuration, Sql.listOf(this::mapSnapshotActionData), type.name(), status.name());
    }

    private SnapshotActionData mapSnapshotActionData(ResultSet rs) throws SQLException {
        String actionId = rs.getString("id");
        UUID commandId = java.util.UUID.fromString(rs.getString("command_id"));
        UUID snapshotId = java.util.UUID.fromString(rs.getString("snapshot_id"));

        SnapshotActionData snapshotActionData = new SnapshotActionData(actionId, commandId, snapshotId);
        return snapshotActionData;
    }

    @Override
    public List<VmActionData> getVmsByActionStatus(ActionStatus status, long thresholdInMinutes) {
        String interval = "INTERVAL '" + thresholdInMinutes + " minutes'  ";
        String selectDateOrderedVmsByActionStatusAndDuration = selectByActionStatusAndDuration + interval + orderby;
        return Sql.with(reportsDataSource)
                .exec(selectDateOrderedVmsByActionStatusAndDuration, Sql.listOf(this::mapVmActionDataWithActionType), status.name());
    }

    private VmActionData mapVmActionDataWithActionType(ResultSet rs) throws SQLException {

        String actionId = rs.getString("id");
        UUID commandId = java.util.UUID.fromString(rs.getString("command_id"));
        UUID vmId = java.util.UUID.fromString(rs.getString("vm_id"));
        String actionType = ActionType.valueOf(rs.getString("type")).name();

        VmActionData vmActionData = new VmActionData(actionId, commandId, vmId, actionType);
        return vmActionData;
    }

}
