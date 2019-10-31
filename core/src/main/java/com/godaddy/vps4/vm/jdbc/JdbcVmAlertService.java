package com.godaddy.vps4.vm.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.vm.VmAlertService;
import com.godaddy.vps4.vm.VmMetric;
import com.godaddy.vps4.vm.VmMetricAlert;

public class JdbcVmAlertService implements VmAlertService {

    private final DataSource dataSource;

    @Inject
    public JdbcVmAlertService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<VmMetricAlert> getVmMetricAlertList(UUID vmId) {
        return Sql.with(dataSource).exec(
                "SELECT m.name, t.type, a.id FROM metric m"
                + " JOIN metric_type t ON m.metric_type_id = t.id"
                + " LEFT JOIN (SELECT id, metric_id FROM vm_silenced_alert WHERE vm_id = ?) a"
                + "   ON a.metric_id = m.id;",
                Sql.listOf(this::mapVmMetricAlert), vmId);
    }

    private VmMetricAlert mapVmMetricAlert(ResultSet rs) throws SQLException {
        VmMetricAlert alert = new VmMetricAlert();
        alert.metric = VmMetric.valueOf(rs.getString("name"));
        alert.type = rs.getString("type");
        alert.status = rs.getInt("id") == 0 ? "enabled" : "disabled";
        return alert;
    }

    @Override
    public VmMetricAlert getVmMetricAlert(UUID vmId, String metric) {
        return Sql.with(dataSource).exec(
                "SELECT m.name, t.type, a.id FROM metric m"
                + " JOIN metric_type t ON m.metric_type_id = t.id"
                + " LEFT JOIN (SELECT id, metric_id FROM vm_silenced_alert WHERE vm_id = ?) a"
                + "   ON a.metric_id = m.id"
                + " WHERE m.name = ?;",
                Sql.nextOrNull(this::mapVmMetricAlert), vmId, metric);
    }

    @Override
    public void disableVmMetricAlert(UUID vmId, String metric) {
        Sql.with(dataSource).exec(
                "INSERT INTO vm_silenced_alert (vm_id, metric_id)"
                + " SELECT ?, (SELECT id FROM metric WHERE name = ?)"
                + " ON CONFLICT DO NOTHING;",  // already disabled: not an error
                null, vmId, metric);
    }

    @Override
    public void reenableVmMetricAlert(UUID vmId, String metric) {
        Sql.with(dataSource).exec(
                "DELETE FROM vm_silenced_alert USING metric WHERE metric_id = metric.id"
                + " AND vm_id = ? and metric.name = ?;",
                null, vmId, metric);
    }

}
