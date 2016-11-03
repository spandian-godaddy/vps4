package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.vps4.jdbc.Sql;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.ImageService;

public class JdbcImageService implements ImageService {
    private final DataSource dataSource;

    private final String tableName = "image";

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

    @Override
    public int getImageId(String name) {
        return Sql.with(dataSource).exec("SELECT image_id FROM " + tableName + " WHERE name=?",
                Sql.nextOrNull(rs -> rs.getInt("image_id")), name);
    }

    @Override
    public Image getImage(String name) {
        return Sql.with(dataSource).exec("SELECT image_id, name, control_panel_id, os_type_id FROM " + tableName + " WHERE name=?",
                Sql.nextOrNull(this::mapImage), name);
    }

    private Image mapImage(ResultSet rs) throws SQLException {
        if (rs == null)
            return null;

        Image image = new Image();

        image.imageName = rs.getString("name");
        image.imageId = rs.getLong("image_id");
        image.controlPanel = ControlPanel.valueOf(rs.getInt("control_panel_id"));
        image.operatingSystem = OperatingSystem.valueOf(rs.getInt("os_type_id"));

        return image;

    }

}
