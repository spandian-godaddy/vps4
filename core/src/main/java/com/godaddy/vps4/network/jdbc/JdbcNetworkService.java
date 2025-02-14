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
    public IpAddress createIpAddress(long hfsAddressId, UUID vmId, String ipAddress, IpAddressType ipAddressType) {
        return Sql.with(dataSource).exec("INSERT INTO ip_address (hfs_address_id, ip_address, vm_id, ip_address_type_id) " +
                        "VALUES (?, ?::inet, ?, ?) " +
                        "RETURNING ip_address.*, family(ip_address.ip_address)",
                Sql.nextOrNull(IpAddressMapper::mapIpAddress),
                hfsAddressId, ipAddress, vmId, ipAddressType.getId());

    }

    @Override
    public void destroyIpAddress(long addressId) {
        Sql.with(dataSource).exec("UPDATE ip_address SET valid_until = now_utc() WHERE address_id = ?",
                null, addressId);
    }

    @Override
    public void activateIpAddress(long addressId) {
        Sql.with(dataSource).exec("UPDATE ip_address SET valid_until = 'infinity' WHERE address_id = ?",
                null, addressId);
    }

    @Override
    public IpAddress getIpAddress(long addressId) {
        return Sql.with(dataSource).exec(
                "SELECT ip.*, family(ip.ip_address) FROM ip_address ip " + " WHERE address_id = ?",
                Sql.nextOrNull(IpAddressMapper::mapIpAddress), addressId);
    }

    @Override
    public List<IpAddress> getVmIpAddresses(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT ip.*, family(ip.ip_address) FROM ip_address ip" + " WHERE vm_id=? ",
                Sql.listOf(IpAddressMapper::mapIpAddress), vmId);
    }

    @Override
    public IpAddress getVmPrimaryAddress(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT ip.*, family(ip.ip_address) FROM ip_address ip WHERE vm_id=? AND ip_address_type_id = ?",
                Sql.nextOrNull(IpAddressMapper::mapIpAddress), vmId, IpAddress.IpAddressType.PRIMARY.getId());
    }

    @Override
    public IpAddress getVmPrimaryAddress(long hfsVmId) {
        return Sql.with(dataSource).exec(
                "SELECT ip.*, family(ip.ip_address) FROM ip_address ip JOIN virtual_machine vm on ip.vm_id = vm.vm_id WHERE vm.hfs_vm_id=? AND ip.ip_address_type_id = ?",
                Sql.nextOrNull(IpAddressMapper::mapIpAddress), hfsVmId, IpAddress.IpAddressType.PRIMARY.getId());
    }

    @Override
    public List<IpAddress> getVmActiveSecondaryAddresses(long hfsVmId) {
        return Sql.with(dataSource).exec(
                "SELECT ip.*, family(ip.ip_address) FROM ip_address ip JOIN virtual_machine vm on ip.vm_id = vm.vm_id WHERE vm.hfs_vm_id=?" +
                        " AND ip.ip_address_type_id = ? and ip.valid_until > now_utc()",
                Sql.listOf(IpAddressMapper::mapIpAddress), hfsVmId, IpAddress.IpAddressType.SECONDARY.getId());
    }

    @Override
    public List<IpAddress> getAllVmSecondaryAddresses(long hfsVmId) {
        return Sql.with(dataSource).exec(
                "SELECT ip.*, family(ip.ip_address) FROM ip_address ip JOIN virtual_machine vm on ip.vm_id = vm.vm_id WHERE vm.hfs_vm_id=?" +
                        " AND ip.ip_address_type_id = ?",
                Sql.listOf(IpAddressMapper::mapIpAddress), hfsVmId, IpAddress.IpAddressType.SECONDARY.getId());
    }

    @Override
    public int getActiveIpAddressesCount(UUID vmId, int internetProtocolVersion) {
        return Sql.with(dataSource).exec(
                "SELECT COUNT(*) FROM ip_address ip WHERE ip.valid_until > now_utc() AND" + " vm_id=? AND family(ip.ip_address) = ?",
                Sql.nextOrNull(rs -> rs.getInt("count")), vmId, internetProtocolVersion);
    }

    @Override
    public List<IpAddress> getActiveIpAddresses(long hfsVmId, int internetProtocolVersion) {
        return Sql.with(dataSource).exec(
                "SELECT ip.*, family(ip.ip_address) FROM ip_address ip JOIN virtual_machine vm on ip.vm_id = vm.vm_id WHERE vm.hfs_vm_id=?" +
                        " AND ip.ip_address_type_id = ? and ip.valid_until > now_utc() AND family(ip.ip_address) = ?",
                Sql.listOf(IpAddressMapper::mapIpAddress), hfsVmId, IpAddress.IpAddressType.SECONDARY.getId(), internetProtocolVersion);
    }

    @Override
    public IpAddress getActiveIpAddressOfVm(UUID vmId, String ipAddress) {
        return Sql.with(dataSource).exec(
                "SELECT ip.*, family(ip.ip_address) FROM ip_address ip " + " WHERE vm_id = ? AND ip_address = ?::inet and valid_until > now_utc()",
                Sql.nextOrNull(IpAddressMapper::mapIpAddress), vmId, ipAddress);
    }

}
