package com.godaddy.vps4.snapshot.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

import com.godaddy.vps4.util.TimestampUtils;

public class JdbcSnapshotActionService implements ActionService {

    private final DataSource dataSource;

    @Inject
    public JdbcSnapshotActionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long createAction(UUID snapshotId, ActionType actionType, String request, long userId, String initiatedBy) {
        return Sql.with(dataSource).exec("INSERT INTO snapshot_action (snapshot_id, action_type_id, request, vps4_user_id, initiated_by) "
                + "VALUES (?, ?, ?::json, ?, ?) RETURNING id;",
                Sql.nextOrNull(rs -> rs.getLong("id")), snapshotId, actionType.getActionTypeId(), request, userId, initiatedBy);
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
    public ResultSubset<Action> getActions(UUID snapshotId, long limit, long offset){
        throw new UnsupportedOperationException("Not implemented, yet");
    }

    @Override
    public ResultSubset<Action> getActions(UUID snapshotId, long limit, long offset, ActionType actionType){
        throw new UnsupportedOperationException("Not implemented, yet");
    }

    @Override
    public ResultSubset<Action> getActions(UUID snapshotId, long limit, long offset, List<String> statusList){
        throw new UnsupportedOperationException("Not implemented, yet");
    }

    @Override
    public ResultSubset<Action> getActions(UUID snapshotId, long limit, long offset, List<String> statusList, Date beginDate, Date endDate){
        throw new UnsupportedOperationException("Not implemented, yet");
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


        return new Action(rs.getLong("id"), snapshotId, type, rs.getLong("vps4_user_id"),
                rs.getString("request"), rs.getString("state"), rs.getString("response"), status,
                rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant(),
                completed, rs.getString("note"), commandId, rs.getString("initiated_by"));
    }

    @Override
    public void tagWithCommand(long actionId, UUID commandId) {
        Sql.with(dataSource).exec("UPDATE snapshot_action SET command_id=? WHERE id=?",
                null, commandId, actionId);
    }

}
