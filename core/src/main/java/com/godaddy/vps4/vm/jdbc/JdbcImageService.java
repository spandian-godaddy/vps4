package com.godaddy.vps4.vm.jdbc;

import java.util.Set;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.ImageService;

public class JdbcImageService implements ImageService {
    private final DataSource dataSource;

    private final String tableName = "compatible_image";

    @Inject
    public JdbcImageService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Set<String> obtainCompatibleImages() {
        return Sql.with(dataSource).exec("SELECT name FROM " + this.tableName,
                Sql.setOf(rs -> rs.getString("name")));
    }

    @Override
    public void addCompatibleImage(String name, Long controlPanelId) {
        Sql.with(dataSource)
                .exec("INSERT INTO " + this.tableName
                        + " (name, control_panel_id) VALUES (?, ?)", null,
                        name, controlPanelId);
    }

    @Override
    public void removeCompatibleImage(String name) {
        Sql.with(dataSource).exec(
                "DELETE FROM " + this.tableName + " WHERE name=?",
                null, name);
    }

}
