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

        String vmIdString = rs.getString("vm_id");
        UUID vmId = (vmIdString == null) ? null : UUID.fromString(vmIdString);

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

        return new Action(rs.getLong("id"), vmId, type, rs.getString("request"), rs.getString("state"),
                rs.getString("response"), status, rs.getTimestamp("created", TimestampUtils.utcCalendar).toInstant(),
                completed, rs.getString("note"), commandId, rs.getString("initiated_by"));
    }

    private UUID mapVmIds(ResultSet rs) throws SQLException {
        String vmIdString = rs.getString("vm_id");
        return (vmIdString == null) ? null : UUID.fromString(vmIdString);
    }

    @Override
    public void tagWithCommand(long actionId, UUID commandId) {
        Sql.with(dataSource).exec("UPDATE vm_action SET command_id=? WHERE id=?",
                null, commandId, actionId);
    }

    @Override
    public List<Action> getIncompleteActions(int minimumAttempts, String action) {
        // get the vmIds of all VMs where the number of failed specified actions is >= minimumAttempts
        List<UUID> vmIds = Sql.with(dataSource).exec(
                "SELECT va.vm_id AS vm_id, count(*) AS attempts " +
                        "FROM vm_action va " +
                        "JOIN action_type t ON va.action_type_id = t.type_id " +
                        "JOIN action_status s ON va.status_id = s.status_id " +
                        "WHERE t.type = '" + action + "' AND s.status != 'COMPLETE' " +
                        "AND va.vm_id NOT IN (" +
                        "SELECT vm_id AS action_vm_id " +
                        "FROM vm_action va " +
                        "JOIN action_type t " +
                        "ON va.action_type_id = t.type_id " +
                        "JOIN action_status s " +
                        "ON va.status_id = s.status_id " +
                        "WHERE t.type = '" + action + "' " +
                        "AND s.status = 'COMPLETE'" +
                        ") GROUP BY va.vm_id HAVING Count(*) >= ?;",
                Sql.listOf(this::mapVmIds), minimumAttempts
        );
        if (vmIds.size() > 0) {
            StringBuilder placeholders = new StringBuilder("?");
            for (int i = 1; i < vmIds.size(); i++) {
                placeholders.append(",?");
            }
            // find most recent specified action that was not successfully completed for each vm
            return Sql.with(dataSource).exec("SELECT a.*, status, type"
                            + " FROM ("
                            + "     SELECT MAX(id) AS id, vm_id FROM vm_action a"
                            + "     JOIN action_type t ON t.type_id=a.action_type_id"
                            + "     WHERE type = '" + action + "'"
                            + "     GROUP BY vm_id"
                            + " ) AS last_actions"
                            + " JOIN vm_action a ON a.id=last_actions.id"
                            + " JOIN action_status s ON s.status_id=a.status_id"
                            + " JOIN action_type t ON t.type_id=a.action_type_id"
                            + " WHERE s.status != 'COMPLETE'"
                            + " AND a.vm_id IN (" + placeholders + ")",
                    Sql.listOf(this::mapAction), vmIds.toArray());
        }
        return new ArrayList<>();
    }

    @Override
    public List<Action> getCreatesWithoutPanopta(long windowSize) {
        return Sql.with(dataSource).exec(
                "SELECT va.*, acs.status, act.type " +
                        "FROM vm_action va " +
                        "JOIN action_type act ON va.action_type_id = act.type_id " +
                        "JOIN action_status acs ON va.status_id = acs.status_id " +
                        "LEFT JOIN panopta_server ps ON va.vm_id = ps.vm_id " +
                        "WHERE ps.vm_id IS NULL " +
                        "AND act.type = 'CREATE_VM' " +
                        "AND acs.status = 'COMPLETE' " +
                        "ORDER BY va.created DESC LIMIT ?",
                Sql.listOf(this::mapAction), windowSize);
    }
}
