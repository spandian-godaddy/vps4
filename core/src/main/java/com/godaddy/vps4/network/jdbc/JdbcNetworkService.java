package com.godaddy.vps4.network.jdbc;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
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
    public void createIpAddress(long ipAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType) {
        Sql.with(dataSource).exec("INSERT INTO ip_address (ip_address_id, ip_address, vm_id, ip_address_type_id) VALUES (?, ?::inet, ?, ?);", null,
                ipAddressId, ipAddress, vmId, ipAddressType.getId());

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
                Sql.nextOrNull(IpAddressMapper::mapIpAddress), ipAddressId);
    }

    @Override
    public List<IpAddress> getVmIpAddresses(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip " + " WHERE vm_id=? ",
                Sql.listOf(IpAddressMapper::mapIpAddress), vmId);
    }

    @Override
    public IpAddress getVmPrimaryAddress(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip WHERE vm_id=? AND ip_address_type_id = ?",
                Sql.nextOrNull(IpAddressMapper::mapIpAddress), vmId, IpAddress.IpAddressType.PRIMARY.getId());
    }

    @Override
    public IpAddress getVmPrimaryAddress(long hfsVmId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip JOIN virtual_machine vm on ip.vm_id = vm.vm_id WHERE vm.hfs_vm_id=? AND ip.ip_address_type_id = ?",
                Sql.nextOrNull(IpAddressMapper::mapIpAddress), hfsVmId, IpAddress.IpAddressType.PRIMARY.getId());
    }


    @Override
    public List<IpAddress> getVmSecondaryAddress(long hfsVmId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip JOIN virtual_machine vm on ip.vm_id = vm.vm_id WHERE vm.hfs_vm_id=?" +
                        " AND ip.ip_address_type_id = ? and ip.valid_until > now_utc()",
                Sql.listOf(IpAddressMapper::mapIpAddress), hfsVmId, IpAddress.IpAddressType.SECONDARY.getId());
    }

    @Override
    public void updateIpWithCheckId(long addressId, long checkId) {
        Sql.with(dataSource).exec("UPDATE ip_address SET ping_check_id = ? WHERE ip_address_id = ?;", null, checkId, addressId);

    }

    @Override
    public void updateIpWithCheckId(String ipAddress, long checkId) {
        Sql.with(dataSource).exec("UPDATE ip_address SET ping_check_id = ? WHERE ip_address = ? and valid_until = 'infinity';", null, checkId, ipAddress);

    }
}
