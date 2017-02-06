package com.godaddy.vps4.network.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import com.godaddy.vps4.network.IpAddress;

public class IpAddressMapper {

    public static IpAddress mapIpAddress(ResultSet rs) throws SQLException {

        long mailRelayId = rs.getLong("mail_relay_id");

        return new IpAddress(rs.getLong("ip_address_id"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("ip_address"),
                IpAddress.IpAddressType.valueOf(rs.getInt("ip_address_type_id")),
                rs.getTimestamp("valid_on").toInstant(),
                rs.getTimestamp("valid_until").toInstant(),
                mailRelayId == 0 ? null : mailRelayId);
    }
}
