package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.OsTypeService;

public class JdbcOsTypeService implements OsTypeService {
	
	private final DataSource dataSource;
	
	@Inject
    public JdbcOsTypeService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

	@Override
	public Long getOsTypeId(String osType) {
		return Sql.with(dataSource).exec("SELECT os_type_id FROM os_type WHERE name=?", this::osTypeIdMapper, osType);
	}

	public Long osTypeIdMapper(ResultSet rs) throws SQLException {
		try{
			rs.next();
			return rs.getLong("os_type_id");
		} catch (SQLException sqlEx) {
			throw new IllegalArgumentException("OS Type does not exist");
		}
	}
}
