package com.godaddy.vps4.panopta.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.util.TimestampUtils;

public class PanoptaDetailMapper {

    public static PanoptaDetail mapPanoptaDetails(ResultSet rs) throws SQLException {

        return new PanoptaDetail(
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("partner_customer_key"),
                rs.getString("customer_key"),
                rs.getInt("server_id"),
                rs.getString("server_key"),
                rs.getTimestamp("created").toInstant(),
                rs.getTimestamp("destroyed") == null ? null :
                rs.getTimestamp("destroyed", TimestampUtils.utcCalendar).toInstant(),
                rs.getString("template_id"));
    }
}
