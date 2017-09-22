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
import com.godaddy.vps4.vm.VmUserType;

public class JdbcVmUserService implements VmUserService{
    private final DataSource dataSource;

    @Inject
    public JdbcVmUserService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createUser(String username, UUID vmId, boolean adminEnabled, VmUserType vmUserType) {
        Sql.with(dataSource).exec("INSERT INTO vm_user (name, admin_enabled, vm_id, vm_user_type_id) VALUES (?, ?, ?, ?)",
                null, username, adminEnabled, vmId, vmUserType.getVmUserTypeId());
    }

    @Override
    public void createUser(String username, UUID vmId, boolean adminEnabled){
        Sql.with(dataSource).exec("INSERT INTO vm_user (name, admin_enabled, vm_id) VALUES (?, ?, ?)",
                null, username, adminEnabled, vmId);
    }

    @Override
    public void createUser(String username, UUID vmId){
        Sql.with(dataSource).exec("INSERT INTO vm_user (name, admin_enabled, vm_id) VALUES (?, ?, ?)",
                null, username, false, vmId);
    }

    @Override
    public void deleteUser(String username, UUID vmId) {
        Sql.with(dataSource).exec("DELETE FROM vm_user WHERE vm_id=? and name=?", null, vmId, username);
    }

    @Override
    public List<VmUser> listUsers(UUID vmId, VmUserType type){
        String base = "SELECT u.name, u.vm_id, u.admin_enabled, ut.type_name"
                + " FROM vm_user u JOIN vm_user_type ut ON u.vm_user_type_id = ut.type_id"
                + " WHERE u.vm_id=?";
        if (type == null) {
            return Sql.with(dataSource).exec(base, Sql.listOf(this::mapUser), vmId);
        } else {
            return Sql.with(dataSource).exec(base + " AND ut.type_name=?", Sql.listOf(this::mapUser), vmId, type.name());
        }
    }

    // Right now we only have one user (of type customer) per vm so returning the first user returned by the query works
    // In the future we may need to possibly tag users of type customer
    @Override
    public VmUser getPrimaryCustomer(UUID vmId) {
        VmUserType userType = VmUserType.CUSTOMER;
        String query = "SELECT u.name, u.vm_id, u.admin_enabled, ut.type_name"
                + " FROM vm_user u JOIN vm_user_type ut ON u.vm_user_type_id = ut.type_id"
                + " WHERE u.vm_id=? AND ut.type_name=?";
        List<VmUser> customers = Sql.with(dataSource).exec(query, Sql.listOf(this::mapUser), vmId, userType.name());
        return customers.get(0);
    }

    @Override
    public VmUser getSupportUser(UUID vmId) {
        return Sql.with(dataSource)
                .exec("SELECT u.name, u.vm_id, u.admin_enabled, ut.type_name"
                        + " FROM vm_user u JOIN vm_user_type ut ON u.vm_user_type_id = ut.type_id"
                        + " WHERE ut.type_name='SUPPORT' AND u.vm_id=?", Sql.nextOrNull(this::mapUser), vmId);
    }

    @Override
    public void updateUserAdminAccess(String username, UUID vmId, boolean adminEnabled){
        Sql.with(dataSource).exec("UPDATE vm_user SET admin_enabled=? WHERE name=? AND vm_id=?", null, adminEnabled, username, vmId);
    }

    protected VmUser mapUser(ResultSet rs) throws SQLException{
        return new VmUser(rs.getString("name"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getBoolean("admin_enabled"),
                VmUserType.valueOf(rs.getString("type_name")));
    }

    @Override
    public boolean userExists(String username, UUID vmId){
        List<VmUser> users = Sql.with(dataSource)
                .exec("SELECT u.name, u.vm_id, u.admin_enabled, ut.type_name"
                        + " FROM vm_user u JOIN vm_user_type ut ON u.vm_user_type_id = ut.type_id"
                        + " WHERE u.vm_id=? AND u.name=?", Sql.listOf(this::mapUser), vmId, username);
        return users.size() > 0;
    }

}
