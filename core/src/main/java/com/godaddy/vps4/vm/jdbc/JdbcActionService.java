package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.ResultSubset;
import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;


public class JdbcActionService implements ActionService {

    private final DataSource dataSource;

    @Inject
    public JdbcActionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long createAction(UUID vmId, ActionType actionType, String request, long userId) {
        return Sql.with(dataSource).exec("INSERT INTO vm_action (vm_id, action_type_id, request, vps4_user_id) "
                + "VALUES (?, ?, ?::json, ?) RETURNING id;",
                Sql.nextOrNull(rs -> rs.getLong("id")), vmId, actionType.getActionTypeId(), request, userId);
    }

    @Override
    public void completeAction(long actionId, String response, String notes) {
        Sql.with(dataSource).exec("UPDATE vm_action SET status_id=3, response=?::json, note=? WHERE id=?",
                null, response, notes, actionId);
    }

    @Override
    public void failAction(long actionId, String response, String notes) {
        Sql.with(dataSource).exec("UPDATE vm_action SET status_id=4, response=?::json, note=? WHERE id=?",
                null, response, notes, actionId);
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
    public Action getVmAction(UUID vmId, long actionId) {
        return Sql.with(dataSource).exec("SELECT * FROM vm_action "
                + " JOIN action_status on vm_action.status_id = action_status.status_id"
                + " JOIN action_type on vm_action.action_type_id = action_type.type_id"
                + " WHERE id = ?"
                + " AND vm_id = ?;",
                Sql.nextOrNull(this::mapAction), actionId, vmId);
    }

    @Override
    public ResultSubset<Action> getActions(UUID vmId, long limit, long offset){
        return Sql.with(dataSource).exec("SELECT *, count(*) over() as total_rows FROM vm_action "
                + " JOIN action_status on vm_action.status_id = action_status.status_id"
                + " JOIN action_type on vm_action.action_type_id = action_type.type_id"
                + " where vm_id = ?"
                + " ORDER BY created DESC"
                + " LIMIT ? OFFSET ?;",
                Sql.nextOrNull(this::mapActionWithTotal),
                vmId, limit, offset);
    }

    @Override
    public ResultSubset<Action> getActions(UUID vmId, long limit, long offset, List<String> statusList){

        return Sql.with(dataSource).exec("SELECT *, count(*) over() as total_rows FROM vm_action "
                + " JOIN action_status on vm_action.status_id = action_status.status_id"
                + " JOIN action_type on vm_action.action_type_id = action_type.type_id"
                + " where vm_id = ?"
                + " and action_status.status in " + formattedStatusList(statusList)
                + " ORDER BY created DESC"
                + " LIMIT ? OFFSET ?;",
                Sql.nextOrNull(this::mapActionWithTotal),
                vmId, limit, offset);
    }

    private String formattedStatusList(List<String> statusList){
        StringJoiner statusJoiner = new StringJoiner("\', \'", "('", "')");
        for(String status : statusList){
            statusJoiner.add(status.toUpperCase());
        }
        return statusJoiner.toString();
    }

    private ResultSubset<Action> mapActionWithTotal(ResultSet rs) throws SQLException {
       long totalRows = rs.getLong("total_rows");
       List<Action> actions = new ArrayList<>();
       actions.add(mapAction(rs));
       while(rs.next()){
           actions.add(mapAction(rs));
       }
       return new ResultSubset<Action>(actions, totalRows);
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

        return new Action(rs.getLong("id"), vmid, type, rs.getLong("vps4_user_id"),
                rs.getString("request"), rs.getString("state"), rs.getString("response"), status,
                rs.getTimestamp("created").toInstant(), rs.getString("note"), commandId);
    }

    @Override
    public void tagWithCommand(long actionId, UUID commandId) {
        Sql.with(dataSource).exec("UPDATE vm_action SET command_id=? WHERE id=?",
                null, commandId, actionId);
    }

}
