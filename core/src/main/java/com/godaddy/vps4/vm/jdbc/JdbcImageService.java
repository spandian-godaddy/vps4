package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.ImageService;
import com.godaddy.vps4.vm.ServerType;


public class JdbcImageService implements ImageService {
    private final DataSource dataSource;

    private final String tableName = "image";

    @Inject
    public JdbcImageService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<String> obtainCompatibleImages() {
        return Sql.with(dataSource).exec("SELECT name FROM " + this.tableName,
                Sql.listOf(rs -> rs.getString("name")));
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
    public int getImageIdByHfsName(String hfsName) {
        Integer id = Sql.with(dataSource).exec("SELECT image_id FROM " + tableName + " WHERE LOWER(hfs_name)=LOWER(?)",
                Sql.nextOrNull(rs -> rs.getInt("image_id")), hfsName);
        return id != null ? id : 0;
    }

    @Override
    public Image getImageByHfsName(String hfsName) {
        return Sql.with(dataSource).exec("SELECT image_id, name, hfs_name, control_panel_id, os_type_id," +
                        "st.server_type_id, st.server_type, st.platform " +
                        "FROM " + tableName + " AS image " +
                        "JOIN server_type AS st USING(server_type_id)" +
                        "WHERE LOWER(hfs_name)=LOWER(?)",
                Sql.nextOrNull(this::mapImage), hfsName);
    }

    @Override
    public long insertImage(int controlPanelId, int osTypeId, String name, int serverTypeId, String hfsName, boolean importedImage) {
        return Sql.with(dataSource).exec("insert into image (control_panel_id, os_type_id, name, server_type_id, hfs_name, imported_image) values (?, ?, ?, ?, ?, ?) RETURNING image_id;",
                                  Sql.nextOrNull(rs -> rs.getLong("image_id")),
                                  controlPanelId,
                                  osTypeId,
                                  name,
                                  serverTypeId,
                                  hfsName,
                                  importedImage);
    }

    @Override
    public List<Image> getImages(String os, String controlPanel, String hfsName, String platform) {
        return Sql.with(dataSource).exec("SELECT image.image_id, image.name, image.hfs_name, image.control_panel_id, image.os_type_id, "+
                "st.server_type_id, st.server_type, st.platform " +
                " FROM image AS image " +
                " JOIN control_panel AS cp ON image.control_panel_id = cp.control_panel_id " +
                " JOIN os_type AS os ON image.os_type_id = os.os_type_id " +
                " JOIN server_type AS st ON image.server_type_id = st.server_type_id " +
                " WHERE image.valid_until > now_utc() " +
                " AND   (?::text is null or LOWER(os.name) = LOWER(?))" +
                " AND   (?::text is null or LOWER(cp.name) = LOWER(?))" +
                " AND   (?::text is null or LOWER(image.hfs_name) = LOWER(?))" +
                " AND   (?::text is null or LOWER(st.platform) = LOWER(?))",
                Sql.listOf(this::mapImage),
                os, os, controlPanel, controlPanel, hfsName, hfsName,  platform, platform);
    }

    private Image mapImage(ResultSet rs) throws SQLException {
        if (rs == null)
            return null;

        ServerType serverType = mapServerType(rs);
        Image image = new Image();

        image.imageName = rs.getString("name");
        image.hfsName = rs.getString("hfs_name");
        image.imageId = rs.getLong("image_id");
        image.controlPanel = ControlPanel.valueOf(rs.getInt("control_panel_id"));
        image.operatingSystem = OperatingSystem.valueOf(rs.getInt("os_type_id"));
        image.serverType = serverType;

        return image;

    }

    protected ServerType mapServerType(ResultSet rs) throws SQLException {
        ServerType serverType = new ServerType();
        serverType.serverTypeId = rs.getInt("server_type_id");
        serverType.serverType = ServerType.Type.valueOf(rs.getString("server_type"));
        serverType.platform = ServerType.Platform.valueOf(rs.getString("platform"));
        return serverType;
    }

}
