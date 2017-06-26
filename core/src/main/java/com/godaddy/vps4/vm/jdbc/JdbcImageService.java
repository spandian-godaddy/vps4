package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
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

    /**
     * TODO getImage and getImageId receiving 'name' (and interpreted as hfs_name) is a hold-over
     *      from when the HFS image name was passed all the way down from the UI.
     *      Once the UI is passing down an image ID instead of the internal HFS image name, remove
     *      the ability to look up an image by HFS image name
     */
    @Override
    public int getImageId(String name) {
        return Sql.with(dataSource).exec("SELECT image_id FROM " + tableName + " WHERE hfs_name=?",
                Sql.nextOrNull(rs -> rs.getInt("image_id")), name);
    }

    @Override
    public Image getImage(String name) {
        return Sql.with(dataSource).exec("SELECT image_id, name, hfs_name, control_panel_id, os_type_id FROM " + tableName + " WHERE hfs_name=?",
                Sql.nextOrNull(this::mapImage), name);
    }

    @Override
    public Set<Image> getImages(String os, String controlPanel, String hfsName) {
        return Sql.with(dataSource).exec("SELECT image.image_id, image.name, image.hfs_name, image.control_panel_id, image.os_type_id" +
                                           " FROM " + tableName + " AS image" +
                                           " JOIN control_panel AS cp ON image.control_panel_id = cp.control_panel_id" +
                                           " JOIN os_type AS os ON image.os_type_id = os.os_type_id" +
                                           " WHERE image.valid_until > NOW()" +
                                           " AND   (?::text is null or LOWER(os.name) = LOWER(?))" +
                                           " AND   (?::text is null or LOWER(cp.name) = LOWER(?))" +
                                           " AND   (?::text is null or LOWER(image.hfs_name) = LOWER(?))",
                                         Sql.setOf(this::mapImage), os, os, controlPanel, controlPanel, hfsName, hfsName);
    }

    private Image mapImage(ResultSet rs) throws SQLException {
        if (rs == null)
            return null;

        Image image = new Image();

        image.imageName = rs.getString("name");
        image.hfsName = rs.getString("hfs_name");
        image.imageId = rs.getLong("image_id");
        image.controlPanel = ControlPanel.valueOf(rs.getInt("control_panel_id"));
        image.operatingSystem = OperatingSystem.valueOf(rs.getInt("os_type_id"));

        return image;

    }

}
