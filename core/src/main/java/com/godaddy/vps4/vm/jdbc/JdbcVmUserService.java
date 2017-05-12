package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;

public class JdbcVmUserService implements VmUserService{
    private final DataSource dataSource;

    @Inject
    public JdbcVmUserService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createUser(String username, UUID vmId, boolean adminEnabled){
        Sql.with(dataSource).exec("SELECT * from user_create(?,?,?)",
                                    null,
                                    username, vmId, adminEnabled);
    }

    @Override
    public void createUser(String username, UUID vmId){
        Sql.with(dataSource).exec("SELECT * from user_create(?,?,?)",
                                    null,
                                    username, vmId, false);
    }

    @Override
    public void deleteUser(String username, UUID vmId) {
        Sql.with(dataSource).exec("DELETE FROM vm_user WHERE vm_id=? and name=?", null, vmId, username);
    }

    @Override
    public List<VmUser> listUsers(UUID vmId){
        return Sql.with(dataSource)
                .exec("SELECT name, vm_id, admin_enabled"
                        + " FROM vm_user"
                        + " WHERE vm_id=?", Sql.listOf(this::mapUser), vmId);
    }

    @Override
    public void updateUserAdminAccess(String username, UUID vmId, boolean adminEnabled){
        Sql.with(dataSource).exec("UPDATE vm_user SET admin_enabled=? WHERE name=? AND vm_id=?", null, adminEnabled, username, vmId);
    }

    protected VmUser mapUser(ResultSet rs) throws SQLException{
        return new VmUser(rs.getString("name"),
                        UUID.fromString(rs.getString("vm_id")), rs.getBoolean("admin_enabled"));
    }

    @Override
    public boolean userExists(String username, UUID vmId){
        List<VmUser> users = Sql.with(dataSource)
                .exec("SELECT name, vm_id, admin_enabled"
                        + " FROM vm_user"
                        + " WHERE vm_id=? AND name=?", Sql.listOf(this::mapUser), vmId, username);
        return users.size() > 0;
    }

}
