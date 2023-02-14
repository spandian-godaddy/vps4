package com.godaddy.vps4.snapshot.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.util.ActionListFilters;
import com.godaddy.vps4.util.ActionListUtils;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.ActionWithOrionGuid;

public class JdbcSnapshotActionService implements ActionService {

    private final DataSource dataSource;
    private final ActionListUtils actionListUtils;

    @Inject
    public JdbcSnapshotActionService(DataSource dataSource) {
        this.dataSource = dataSource;
        actionListUtils = new ActionListUtils("snapshot_action", "snapshot_id", dataSource);
    }

    @Override
    public long createAction(UUID snapshotId, ActionType actionType, String request, String initiatedBy) {
        return Sql.with(dataSource).exec("INSERT INTO snapshot_action (snapshot_id, action_type_id, request, initiated_by) "
                + "VALUES (?, ?, ?::json, ?) RETURNING id;",
                Sql.nextOrNull(rs -> rs.getLong("id")), snapshotId, actionType.getActionTypeId(), request, initiatedBy);
    }

    @Override
    public void completeAction(long actionId, String response, String notes) {
        Sql.with(dataSource).exec("UPDATE snapshot_action SET status_id=3, response=?::json, note=?, completed=now_utc() WHERE id=?",
                null, response, notes, actionId);
    }

    @Override
    public void failAction(long actionId, String response, String notes) {
        Sql.with(dataSource).exec("UPDATE snapshot_action SET status_id=4, response=?::json, note=? WHERE id=?",
                null, response, notes, actionId);
    }

    @Override
    public void cancelAction(long actionId, String response, String notes) {
        Sql.with(dataSource).exec("UPDATE snapshot_action SET status_id=5, response=?::json, note=? WHERE id=?",
                null, response, notes, actionId);
    }

    @Override
    public List<Action> getIncompleteActions(UUID snapshotId) {
        return Sql.with(dataSource).exec("SELECT * FROM snapshot_action "
                        + " JOIN action_status on snapshot_action.status_id = action_status.status_id"
                        + " JOIN action_type on snapshot_action.action_type_id = action_type.type_id"
                        + " where snapshot_action.snapshot_id = ? and action_status.status in ('NEW', 'IN_PROGRESS')",
                Sql.listOf(this::mapAction), snapshotId);
    }

    @Override
    public void markActionInProgress(long actionId) {
        Sql.with(dataSource).exec("UPDATE snapshot_action SET status_id=2 WHERE id=?",
                null, actionId);
    }

    @Override
    public void updateActionState(long actionId, String state) {
        Sql.with(dataSource).exec("UPDATE snapshot_action SET state=?::json WHERE id=?",
                null, state, actionId);
    }

    @Override
    public Action getAction(long actionId) {
        return Sql.with(dataSource).exec("SELECT * FROM snapshot_action "
                + " JOIN action_status on snapshot_action.status_id = action_status.status_id"
                + " JOIN action_type on snapshot_action.action_type_id = action_type.type_id"
                + " where id = ?;",
                Sql.nextOrNull(this::mapAction), actionId);
    }

    @Override
    public Action getAction(UUID snapshotId, long actionId) {
        return Sql.with(dataSource).exec("SELECT * FROM snapshot_action "
                        + " JOIN action_status on snapshot_action.status_id = action_status.status_id"
                        + " JOIN action_type on snapshot_action.action_type_id = action_type.type_id"
                        + " WHERE id = ?"
                        + " AND snapshot_id = ?;",
                Sql.nextOrNull(this::mapAction), actionId, snapshotId);
    }

    @Override
    public List<Action> getActions(UUID snapshotId) {
        return Sql.with(dataSource).exec("SELECT * FROM snapshot_action "
                        + " JOIN action_status on snapshot_action.status_id = action_status.status_id"
                        + " JOIN action_type on snapshot_action.action_type_id = action_type.type_id"
                        + " AND snapshot_id = ?;",
                Sql.listOf(this::mapAction), snapshotId);
    }

    @Override
    public ResultSubset<Action> getActionList(ActionListFilters actionFilters) {
        return actionListUtils.getActions(actionFilters);
    }

    private Action mapAction(ResultSet rs) throws SQLException {
        ActionStatus status = ActionStatus.valueOf(rs.getString("status"));
        ActionType type = ActionType.valueOf(rs.getString("type"));

        String snapshotIdStr = rs.getString("snapshot_id");
        UUID snapshotId = null;
        if (snapshotIdStr != null){
            snapshotId = UUID.fromString(snapshotIdStr);
        }

        String commandIdStr = rs.getString("command_id");
        UUID commandId = null;
        if (commandIdStr != null) {
            commandId = UUID.fromString(commandIdStr);
        }

        Timestamp completedTs = rs.getTimestamp("completed", TimestampUtils.utcCalendar);
        Instant completed = null;
        if (completedTs != null){
            completed = completedTs.toInstant();
        }

        return new Action(rs.getLong("id"), snapshotId, type, rs.getString("request"), rs.getString("state"),
                rs.getString("response"), status, rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant(),
                completed, rs.getString("note"), commandId, rs.getString("initiated_by"));
    }

    @Override
    public void tagWithCommand(long actionId, UUID commandId) {
        Sql.with(dataSource).exec("UPDATE snapshot_action SET command_id=? WHERE id=?",
                null, commandId, actionId);
    }

    @Override
    public List<Action> getIncompleteActions(int minimumAttempts, String action) {
        throw new UnsupportedOperationException("Not yet implemented for snapshots");
    }

    @Override
    public List<ActionWithOrionGuid> getCreatesWithoutPanopta(long windowSize) {
        throw new UnsupportedOperationException("Not yet implemented for snapshots");
    }

    @Override
    public List<ActionWithOrionGuid> getActionsForFailedPercentMonitor(long windowSize) {
        throw new UnsupportedOperationException("Not yet implemented for snapshots");
    }

    @Override
    public List<String> getVmActionTypes(UUID vmId) {
        throw new UnsupportedOperationException("Not yet implemented for snapshots");
    }
}
