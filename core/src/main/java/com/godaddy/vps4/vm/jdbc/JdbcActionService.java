package com.godaddy.vps4.vm.jdbc;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcActionService implements ActionService{

    private final DataSource dataSource;

    @Inject
    public JdbcActionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public long createAction(long vmId, ActionType actionType, String request, long userId){
       return Sql.with(dataSource).exec("INSERT INTO vm_action (virtual_machine_id, action_type_id, request, vps4_user_id) VALUES (?, ?, ?::json, ?) RETURNING id;",
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
                null,
                actionId);
    }

    @Override
    public void updateActionState(long actionId, String state) {
        Sql.with(dataSource).exec("UPDATE vm_action SET state=? WHERE id=?",
                null,
                state, actionId);
    }

    @Override
    public Action getAction(long actionId){
        return Sql.with(dataSource).exec("SELECT *  FROM vm_action "
                + " JOIN action_status on vm_action.status_id = action_status.status_id"
                + " JOIN action_type on vm_action.action_type_id = action_type.type_id"
                + " where id = ?;",
                Sql.nextOrNull(this::mapAction), actionId);
    }

    private Action mapAction(ResultSet rs) throws SQLException {
        ActionStatus status = ActionStatus.valueOf(rs.getString("status"));
        ActionType type = ActionType.valueOf(rs.getString("type"));
        return new Action(rs.getLong("id"), rs.getLong("virtual_machine_id"), type, rs.getLong("vps4_user_id"),
                          rs.getString("request"), rs.getString("state"), rs.getString("response"), status,
                          rs.getTimestamp("created").toInstant(), rs.getString("note"));
    }

}
