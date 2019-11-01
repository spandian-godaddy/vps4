package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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

public class JdbcVmActionService implements ActionService {

    private final DataSource dataSource;
    private final ActionListUtils actionListUtils;

    @Inject
    public JdbcVmActionService(DataSource dataSource) {
        this.dataSource = dataSource;
        actionListUtils = new ActionListUtils("vm_action", "vm_id", dataSource);
    }

    @Override
    public long createAction(UUID vmId, ActionType actionType, String request, String initiatedBy) {
        return Sql.with(dataSource).exec("INSERT INTO vm_action (vm_id, action_type_id, request, initiated_by) "
                + "VALUES (?, ?, ?::json, ?) RETURNING id;",
                Sql.nextOrNull(rs -> rs.getLong("id")), vmId, actionType.getActionTypeId(), request, initiatedBy);
    }

    @Override
    public void completeAction(long actionId, String response, String notes) {
        Sql.with(dataSource).exec("UPDATE vm_action SET status_id=3, response=?::json, note=?, completed=now_utc() WHERE id=?",
                null, response, notes, actionId );
    }

    @Override
    public void failAction(long actionId, String response, String notes) {
        Sql.with(dataSource).exec("UPDATE vm_action SET status_id=4, response=?::json, note=? WHERE id=?",
                null, response, notes, actionId);
    }

    @Override
    public void cancelAction(long actionId, String response, String notes) {
        Sql.with(dataSource).exec("UPDATE vm_action SET status_id=5, response=?::json, note=? WHERE id=?",
                null, response, notes, actionId);
    }

    @Override
    public List<Action> getIncompleteActions(UUID vmId) {
        ActionListFilters actionFilters = new ActionListFilters();
        actionFilters.byStatus(ActionStatus.NEW, ActionStatus.IN_PROGRESS);
        actionFilters.byResourceId(vmId);
        ResultSubset<Action> result = getActionList(actionFilters);
        return result != null ? result.results : new ArrayList<>();
    }

    @Override
    public void markActionInProgress(long actionId) {
        Sql.with(dataSource).exec("UPDATE vm_action SET status_id=2 WHERE id=?",
                null, actionId);
    }

    @Override
    public void updateActionState(long actionId, String state) {
        Sql.with(dataSource).exec("UPDATE vm_action SET state=?::json WHERE id=?",
                null, state, actionId);
    }

    @Override
    public Action getAction(long actionId) {
        return Sql.with(dataSource).exec("SELECT * FROM vm_action "
                + " JOIN action_status on vm_action.status_id = action_status.status_id"
                + " JOIN action_type on vm_action.action_type_id = action_type.type_id"
                + " where id = ?;",
                Sql.nextOrNull(this::mapAction), actionId);
    }

    @Override
    public Action getAction(UUID vmId, long actionId) {
        return Sql.with(dataSource).exec("SELECT * FROM vm_action "
                + " JOIN action_status on vm_action.status_id = action_status.status_id"
                + " JOIN action_type on vm_action.action_type_id = action_type.type_id"
                + " WHERE id = ?"
                + " AND vm_id = ?;",
                Sql.nextOrNull(this::mapAction), actionId, vmId);
    }

    @Override
    public ResultSubset<Action> getActionList(ActionListFilters actionFilters) {
        return actionListUtils.getActions(actionFilters);
    }

    private Action mapAction(ResultSet rs) throws SQLException {
        ActionStatus status = ActionStatus.valueOf(rs.getString("status"));
        ActionType type = ActionType.valueOf(rs.getString("type"));

        String vmIdStr = rs.getString("vm_id");
        UUID vmid = null;
        if (vmIdStr != null){
            vmid = UUID.fromString(vmIdStr);
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

        return new Action(rs.getLong("id"), vmid, type, rs.getString("request"), rs.getString("state"),
                rs.getString("response"), status, rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant(),
                completed, rs.getString("note"), commandId, rs.getString("initiated_by"));
    }

    @Override
    public void tagWithCommand(long actionId, UUID commandId) {
        Sql.with(dataSource).exec("UPDATE vm_action SET command_id=? WHERE id=?",
                null, commandId, actionId);
    }

}
