package com.godaddy.vps4.move.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.move.VmMoveImageMap;
import com.godaddy.vps4.move.VmMoveImageMapService;
import com.godaddy.vps4.vm.ServerType;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcVmMoveImageMapService implements VmMoveImageMapService {

    private final DataSource dataSource;

    @Inject
    public JdbcVmMoveImageMapService(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public VmMoveImageMap getVmMoveImageMap(int originalImageId, ServerType.Platform toPlatform) {
        return Sql.with(dataSource).exec("SELECT id, from_image_id, to_image_id from vm_move_image_map m " +
                "join virtual_machine_spec s on m.from_spec_id = s.spec_id " +
                "where m.from_spec_id = ? and s.server_type_id = ?",
                Sql.nextOrNull(this::mapGetVmMoveImageMap), originalImageId, toPlatform.getplatformId());
    }

    private VmMoveImageMap mapGetVmMoveImageMap(ResultSet resultSet) throws SQLException {
        VmMoveImageMap map = new VmMoveImageMap();
        map.id = resultSet.getInt("id");
        map.fromImageId = resultSet.getLong("from_image_id");
        map.toImageId = resultSet.getLong("to_image_id");

        return map;
    }
}
