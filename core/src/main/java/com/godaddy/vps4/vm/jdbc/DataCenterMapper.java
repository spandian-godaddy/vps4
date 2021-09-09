package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.godaddy.vps4.vm.DataCenter;

public class DataCenterMapper {

    public static DataCenter mapDataCenter(ResultSet rs) throws SQLException {
        return rs.getInt("data_center_id") != 0 ? new DataCenter(rs.getInt("data_center_id"),
                              rs.getString("description")) : null;
    }
}
