package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;

public class JdbcActionService implements ActionService{
    
    private final DataSource dataSource;

    @Inject
    public JdbcActionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public long createAction(long vmId, String request, long userId){
       return Sql.with(dataSource).exec("INSERT INTO vm_action (virtual_machine_id, request, vps4_user_id) VALUES (?, ?::json, ?) RETURNING id;", 
               Sql.nextOrNull(this::getId), vmId, request, userId);
    }
    
    private long getId(ResultSet rs) throws SQLException{
        return rs.getLong("id");
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
        return new Action(rs.getLong("id"), rs.getLong("virtual_machine_id"), rs.getString("type"), rs.getLong("vps4_user_id"),
                          rs.getString("request"), rs.getString("response"), rs.getString("status"), 
                          rs.getTimestamp("created").toInstant(), rs.getString("note"));
    }
    
    @Override
    public void updateAction(long actionId, Map<String, Object> paramsToUpdate){
        if(paramsToUpdate.isEmpty())
            return;
        ArrayList<Object> values = new ArrayList<Object>();
        StringBuilder nameSets = new StringBuilder();
        nameSets.append("UPDATE vm_action action SET ");
        for(Map.Entry<String, Object> pair: paramsToUpdate.entrySet()){
            if(values.size() > 0)
                nameSets.append(", ");
            nameSets.append(pair.getKey());
            nameSets.append("=?");
            values.add(pair.getValue());
        }
        nameSets.append(" WHERE id=?");
        values.add(actionId);
        Sql.with(dataSource).exec(nameSets.toString(), null, values.toArray());
    }
    

}
