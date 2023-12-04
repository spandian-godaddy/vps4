package com.godaddy.vps4.firewall.jdbc;

import com.godaddy.vps4.firewall.model.VmFirewallSite;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.jdbc.IpAddressMapper;
import com.godaddy.vps4.util.TimestampUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class FirewallSiteMapper {

    protected static IpAddress mapIpAddress(ResultSet rs) throws SQLException {
        String ip = rs.getString("ip_address");
        if (StringUtils.isEmpty(ip)){
            return null;
        }

        return IpAddressMapper.mapIpAddress(rs);
    }

    public static VmFirewallSite mapFirewallSite(ResultSet rs) throws SQLException {
        IpAddress ipAddress = mapIpAddress(rs);

        return new VmFirewallSite(
                UUID.fromString(rs.getString("vfs_vm_id")),
                ipAddress,
                rs.getString("domain"),
                rs.getString("site_id"),
                rs.getTimestamp("vfs_valid_on", TimestampUtils.utcCalendar).toInstant(),
                rs.getTimestamp("vfs_valid_until", TimestampUtils.utcCalendar).toInstant());
    }
}
