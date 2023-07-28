package com.godaddy.vps4.move.jdbc;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.move.VmMoveSpecMap;
import com.godaddy.vps4.move.VmMoveSpecMapService;
import com.godaddy.vps4.vm.ServerType;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcVmMoveSpecMapService implements VmMoveSpecMapService {

    private final DataSource dataSource;

    @Inject
    public JdbcVmMoveSpecMapService(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public VmMoveSpecMap getVmMoveSpecMap(int originalSpecId, ServerType.Platform toPlatform) {
        return Sql.with(dataSource).exec("SELECT id, from_spec_id, to_spec_id from vm_move_spec_map " +
                "join virtual_machine_spec on vm_move_spec_map.from_spec_id = virtual_machine_spec.spec_id" +
                "where from_spec_id = ? and virtual_machine_spec.server_type_id = ?",
                Sql.nextOrNull(this::mapGetVmMoveSpecMap), originalSpecId, toPlatform.getplatformId());
    }

    private VmMoveSpecMap mapGetVmMoveSpecMap(ResultSet resultSet) throws SQLException {
        VmMoveSpecMap map = new VmMoveSpecMap();
        map.id = resultSet.getInt("id");
        map.fromSpecId = resultSet.getInt("from_spec_id");
        map.toSpecId = resultSet.getInt("to_spec_id");

        return map;
    }
}
