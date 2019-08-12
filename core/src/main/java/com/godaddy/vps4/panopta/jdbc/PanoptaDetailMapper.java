package com.godaddy.vps4.panopta.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

import com.godaddy.vps4.panopta.PanoptaDetail;

public class PanoptaDetailMapper {

    public static PanoptaDetail mapPanoptaDetails(ResultSet rs) throws SQLException {

        Timestamp destroyed = rs.getTimestamp("destroyed");

        return new PanoptaDetail(
                rs.getLong("panopta_detail_id"),
                UUID.fromString(rs.getString("vm_id")),
                rs.getString("partner_customer_key"),
                rs.getString("customer_key"),
                rs.getLong("server_id"),
                rs.getString("server_key"),
                rs.getTimestamp("created").toInstant(),
                destroyed == null ? null: destroyed.toInstant());
    }
}
