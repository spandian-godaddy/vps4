package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.vm.DataCenter;
import com.godaddy.vps4.vm.DataCenterService;

public class JdbcDataCenterService implements DataCenterService {

    private final DataSource dataSource;

    @Inject
    public JdbcDataCenterService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataCenter getDataCenter(int dataCenterId) {
        return Sql.with(dataSource).exec("SELECT * FROM data_center "
                + " where data_center_id = ?;",
                Sql.nextOrNull(this::mapDataCenter), dataCenterId);
    }

    protected DataCenter mapDataCenter(ResultSet rs) throws SQLException{
    return new DataCenter(rs.getInt("data_center_id"),
            rs.getString("description"));
    }

    @Override
    public List<DataCenter> getDataCentersByReseller(String resellerId) {
        return Sql.with(dataSource).exec("SELECT DISTINCT d.* FROM reseller_data_centers rd "
                + "JOIN data_center d ON rd.data_center_id = d.data_center_id "
                + "WHERE rd.reseller_id= ?;",
                Sql.listOf(this::mapDataCenter), resellerId);
    }

}
