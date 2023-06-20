package com.godaddy.vps4.reseller.jdbc;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.reseller.ResellerService;

import java.util.List;

public class JdbcResellerService implements ResellerService {
    private final DataSource dataSource;

    @Inject
    public JdbcResellerService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String getResellerDescription(String resellerId) {
        return Sql.with(dataSource).exec("SELECT description FROM reseller WHERE reseller_id = ?",
                                         Sql.nextOrNull(rs -> rs.getString("description")), resellerId);
    }

    @Override
    public List<String> getBrandResellerIds() {
        return Sql.with(dataSource).exec("SELECT reseller_id FROM reseller",
                Sql.listOf(rs -> rs.getString("reseller_id")));
    }
}
