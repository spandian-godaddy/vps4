package com.godaddy.vps4.network.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.IpAddress.IpAddressType;
import com.godaddy.vps4.network.NetworkService;

public class JdbcNetworkService implements NetworkService {

    private final DataSource dataSource;

    @Inject
    public JdbcNetworkService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createIpAddress(long ipAddressId, UUID vmId, String address, IpAddressType type) {

        Sql.with(dataSource).exec("SELECT * FROM ip_address_create(?,?,?,?)", null,
                ipAddressId, vmId, address, type.getId());

    }

    protected IpAddress mapIpAddress(ResultSet rs) throws SQLException {

        return new IpAddress(rs.getLong("ip_address_id"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("ip_address"),
                IpAddress.IpAddressType.valueOf(rs.getInt("ip_address_type_id")),
                rs.getTimestamp("valid_on").toInstant(),
                rs.getTimestamp("valid_until").toInstant());
    }

    @Override
    public void destroyIpAddress(long ipAddressId) {
        Sql.with(dataSource).exec("SELECT * FROM ip_address_delete(?)", null,
                ipAddressId);
    }

    @Override
    public IpAddress getIpAddress(long ipAddressId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip " + " WHERE ip_address_id=? ",
                Sql.nextOrNull(this::mapIpAddress), ipAddressId);
    }

    @Override
    public List<IpAddress> getVmIpAddresses(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip " + " WHERE vm_id=? ",
                Sql.listOf(this::mapIpAddress), vmId);
    }

    @Override
    public IpAddress getVmPrimaryAddress(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip WHERE vm_id=? AND ip_address_type_id = ?",
                Sql.nextOrNull(this::mapIpAddress), vmId, IpAddress.IpAddressType.PRIMARY.getId());
    }
    
    @Override
    public IpAddress getVmPrimaryAddress(long hfsVmId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip JOIN virtual_machine vm on ip.vm_id = vm.id WHERE vm.hfs_vm_id=? AND ip.ip_address_type_id = ?",
                Sql.nextOrNull(this::mapIpAddress), hfsVmId, IpAddress.IpAddressType.PRIMARY.getId());
    }
}
