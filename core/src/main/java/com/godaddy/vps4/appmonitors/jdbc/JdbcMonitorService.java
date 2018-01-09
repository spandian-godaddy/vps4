package com.godaddy.vps4.appmonitors.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.appmonitors.MonitorService;
import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.vm.ActionType;
import com.google.inject.Inject;

public class JdbcMonitorService implements MonitorService {

    private final DataSource dataSource;

    private final static String selectVmsByActionAndDuration = "SELECT vma.vm_id\n" +
            "FROM vm_action vma\n" +
            "WHERE vma.created < now()\n" +
            "AND vma.action_type_id = (\n" +
            "  SELECT type_id FROM action_type WHERE type = ?\n" +
            ")\n" +
            "AND vma.status_id = (\n" +
            "  SELECT status_id FROM action_status WHERE status = ?\n" +
            ")\n" +
            "AND now() - vma.created >= ";

    private final static String orderby = "ORDER BY vma.created ASC;\n";

    @Inject
    public JdbcMonitorService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<UUID> getVmsByActions(ActionType type, ActionStatus status, long thresholdInMinutes) {
        String interval = "INTERVAL '" + thresholdInMinutes + " minutes' \n";
        String selectDateOrderedVmsByActionAndDuration = selectVmsByActionAndDuration + interval + orderby;
        return Sql.with(dataSource)
                .exec(selectDateOrderedVmsByActionAndDuration, Sql.listOf(this::mapVmUUIDs), type.name(), status.name());
    }

    private UUID mapVmUUIDs(ResultSet rs) throws SQLException {
        UUID vmId = java.util.UUID.fromString(rs.getString("vm_id"));
        return vmId;
    }

}
