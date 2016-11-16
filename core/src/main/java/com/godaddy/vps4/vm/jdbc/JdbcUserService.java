package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.ConnectionProvider;
import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.User;
import com.godaddy.vps4.vm.UserService;

public class JdbcUserService implements UserService{
    private final DataSource dataSource;
    
    @Inject
    public JdbcUserService(DataSource dataSource) {
        this.dataSource = dataSource;
        new ConnectionProvider(dataSource);
    }
    
    @Override
    public void createUser(String username, long vmId, boolean adminEnabled){
        Sql.with(dataSource).exec("SELECT * from user_create(?,?,?)", 
                                    null,  
                                    username, vmId, adminEnabled);
    }
    
    @Override
    public void createUser(String username, long vmId){
        Sql.with(dataSource).exec("SELECT * from user_create(?,?,?)", 
                                    null,  
                                    username, vmId, false);
    }
    
    @Override
    public List<User> listUsers(long vmId){
        return Sql.with(dataSource)
                .exec("SELECT name, virtual_machine_id, admin_enabled"
                        + " FROM vm_user"
                        + " WHERE virtual_machine_id=?", Sql.listOf(this::mapUser), vmId);
    }
    
    @Override
    public void updateUserAdminAccess(String username, long vmId, boolean adminEnabled){
        Sql.with(dataSource).exec("UPDATE vm_user SET admin_enabled=? WHERE name=? AND virtual_machine_id=?", null, adminEnabled, username, vmId);
    }
    
    protected User mapUser(ResultSet rs) throws SQLException{
        return new User(rs.getString("name"), 
                        rs.getLong("virtual_machine_id"), rs.getBoolean("admin_enabled"));
    }
    
    @Override
    public boolean userExists(String username, long vmId){
        List<User> users = Sql.with(dataSource)
                .exec("SELECT name, virtual_machine_id, admin_enabled"
                        + " FROM vm_user"
                        + " WHERE virtual_machine_id=? AND name=?", Sql.listOf(this::mapUser), vmId, username);
        return users.size() > 0;
    }

}
