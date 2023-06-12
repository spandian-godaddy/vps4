package com.godaddy.vps4.network.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.util.TimestampUtils;

public class IpAddressMapper {

    public static IpAddress mapIpAddress(ResultSet rs) throws SQLException {

        return new IpAddress(
                rs.getLong(("address_id")),
                rs.getLong("hfs_address_id"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("ip_address"),
                IpAddress.IpAddressType.valueOf(rs.getInt("ip_address_type_id")),
                rs.getTimestamp("valid_on", TimestampUtils.utcCalendar).toInstant(),
                rs.getTimestamp("valid_until", TimestampUtils.utcCalendar).toInstant(),
                rs.getInt("family"));

    }
}
