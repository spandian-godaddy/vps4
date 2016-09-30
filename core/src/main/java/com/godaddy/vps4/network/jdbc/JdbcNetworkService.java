package com.godaddy.vps4.network.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;

public class JdbcNetworkService implements NetworkService {

    private final DataSource dataSource;

    @Inject
    public JdbcNetworkService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void createIpAddress(long ipAddressId, long projectId) {

        Sql.with(dataSource).exec("SELECT * FROM ip_address_create(?,?)", null,
                ipAddressId, projectId);

    }

    protected IpAddress mapMcsIpAddress(ResultSet rs) throws SQLException {

        return new IpAddress(rs.getLong("ip_address_id"),
                rs.getLong("project_id"),
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
                Sql.nextOrNull(this::mapMcsIpAddress), ipAddressId);
    }

    @Override
    public List<IpAddress> listIpAddresses(long projectId) {
        return Sql.with(dataSource).exec(
                "SELECT * FROM ip_address ip " + " WHERE project_id=? ",
                Sql.listOf(this::mapMcsIpAddress), projectId);
    }
}
