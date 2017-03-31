package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;

import com.godaddy.vps4.vm.ControlPanelService;

public class JdbcControlPanelService implements ControlPanelService {

    private final DataSource dataSource;

    // private final String tableName = "control_panel";

    @Inject
    public JdbcControlPanelService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void addControlPanelType(String controlPanel) {
        Sql.with(dataSource).exec("INSERT INTO control_panel (name) VALUES (?)",
                null, controlPanel);
    }

    @Override
    public int getControlPanelId(String controlPanel) {
        return Sql.with(dataSource).exec(
                    "SELECT (control_panel_id) from control_panel WHERE name=?",
                    this::controlPanelIdMapper,
                    controlPanel);
    }

    public int controlPanelIdMapper(ResultSet rs) throws SQLException {
        try {
            rs.next();
            return rs.getInt("control_panel_id");
        }
        catch (SQLException sqlEx) {
            throw new IllegalArgumentException("Control Panel Does not exist");
        }
    }

    @Override
    public void deleteControlPanelType(String controlPanel) {
        Sql.with(dataSource).exec("DELETE FROM control_panel WHERE name=?", null, controlPanel);

    }

    @Override
    public List<String> listControlPanelTypes() {
        return Sql.with(dataSource).exec("SELECT name FROM control_panel",
                Sql.listOf(rs -> rs.getString("name")));
    }

}
