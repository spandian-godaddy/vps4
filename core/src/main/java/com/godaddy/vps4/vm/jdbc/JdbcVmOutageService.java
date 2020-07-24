package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.util.TimestampUtils;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmOutage;
import com.godaddy.vps4.vm.VmOutageService;

public class JdbcVmOutageService implements VmOutageService {

    private final DataSource dataSource;

    private String selectVmOutageQuery = "SELECT o.id, vm_id, m.name, started, ended, reason, panopta_outage_id"
                + " FROM vm_outage o"
                + " JOIN metric m ON m.id = o.metric_id"
                + " WHERE vm_id = ?";

    @Inject
    public JdbcVmOutageService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<VmOutage> getVmOutageList(UUID vmId) {
        return getVmOutageList(vmId, null, false);
    }

    @Override
    public List<VmOutage> getVmOutageList(UUID vmId, VmMetric metric) {
        return getVmOutageList(vmId, metric, false);
    }

    @Override
    public List<VmOutage> getVmOutageList(UUID vmId, VmMetric metric, boolean activeOnly) {
        StringBuilder query = new StringBuilder();
        query.append(selectVmOutageQuery);
        List<Object> filterValues = new ArrayList<>();
        filterValues.add(vmId);

        if (metric != null) {
            query.append(" AND m.name = ?");
            filterValues.add(metric.name());
        }

        if (activeOnly) {
            query.append(" AND ended IS NULL");
        }

        return Sql.with(dataSource).exec(query.toString(), Sql.listOf(this::mapVmOutage), filterValues.toArray());
    }

    private VmOutage mapVmOutage(ResultSet rs) throws SQLException {
        VmOutage outage = new VmOutage();
        outage.vmId = UUID.fromString(rs.getString("vm_id"));
        outage.outageId = rs.getInt("id");
        outage.metric = VmMetric.valueOf(rs.getString("name"));
        outage.started = rs.getTimestamp("started", TimestampUtils.utcCalendar).toInstant();

        Timestamp end = rs.getTimestamp("ended", TimestampUtils.utcCalendar);
        outage.ended = end != null ? end.toInstant() : null;
        outage.reason = rs.getString("reason");
        outage.outageDetailId = rs.getLong("panopta_outage_id");
        return outage;
    }

    @Override
    public VmOutage getVmOutage(int outageId) {
        return Sql.with(dataSource).exec(
                "SELECT o.id, vm_id, m.name, started, ended, reason, panopta_outage_id"
                + " FROM vm_outage o"
                + " JOIN metric m ON m.id = o.metric_id"
                + " WHERE o.id = ?;",
                Sql.nextOrNull(this::mapVmOutage), outageId);
    }

    @Override
    public VmOutage getVmOutage(UUID vmId, VmMetric metric, long panoptaOutageId) {
        return Sql.with(dataSource).exec(
                "SELECT o.id, vm_id, m.name, started, ended, reason, panopta_outage_id"
                + " FROM vm_outage o"
                + " JOIN metric m ON m.id = o.metric_id"
                + " WHERE vm_id = ? AND m.name = ? AND panopta_outage_id = ?;",
                Sql.nextOrNull(this::mapVmOutage), vmId, metric.name(), panoptaOutageId);
    }

    @Override
    public int newVmOutage(UUID vmId, VmMetric metric, Instant startDate, String reason, long panoptaOutageId) {
        return Sql.with(dataSource).exec(
                "INSERT INTO vm_outage (vm_id, metric_id, started, reason, panopta_outage_id)"
                + " SELECT ?, (SELECT id FROM metric WHERE name = ?), ?, ?, ?"
                + " RETURNING id;",
                Sql.nextOrNull(rs -> rs.getInt("id")), vmId, metric.name(),
                LocalDateTime.ofInstant(startDate, ZoneOffset.UTC),
                reason, panoptaOutageId);
    }

    @Override
    public void clearVmOutage(int outageId, Instant endDate) {
        Sql.with(dataSource).exec(
                "UPDATE vm_outage SET ended = ? WHERE id = ?;",
                null, LocalDateTime.ofInstant(endDate, ZoneOffset.UTC), outageId);
    }

    @Override
    public void clearAllActiveOutagesByMetric(UUID vmId, VmMetric metric, Instant endDate) {
        Sql.with(dataSource).exec(
                "UPDATE vm_outage SET ended = ?"
                + " FROM metric WHERE metric.id = vm_outage.metric_id"
                + " AND ended IS NULL AND vm_id = ? AND metric.name = ?;",
                null, LocalDateTime.ofInstant(endDate, ZoneOffset.UTC), vmId, metric.name());
    }

    @Override
    public List<VmOutage> getVmOutageList(long panoptaOutageId) {
        return Sql.with(dataSource).exec(
                "SELECT o.id, vm_id, m.name, started, ended, reason, panopta_outage_id"
                + " FROM vm_outage o"
                + " JOIN metric m ON m.id = o.metric_id"
                + " WHERE panopta_outage_id = ?;",
                Sql.listOf(this::mapVmOutage), panoptaOutageId);
    }

}
