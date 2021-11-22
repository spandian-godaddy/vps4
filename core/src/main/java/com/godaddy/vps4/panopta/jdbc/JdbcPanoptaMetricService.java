package com.godaddy.vps4.panopta.jdbc;

import java.util.Map;

import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.panopta.PanoptaMetricService;
import com.godaddy.vps4.vm.VmMetric;
import com.google.inject.Inject;

public class JdbcPanoptaMetricService implements PanoptaMetricService {
    private final DataSource dataSource;

    @Inject
    public JdbcPanoptaMetricService(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public Map<Long, VmMetric> getAllMetrics() {
        return Sql.with(dataSource)
                  .exec("SELECT DISTINCT metric_type_id, name FROM metric_type mt JOIN metric m ON mt.metric_id = m.id",
                        Sql.mapOf(rs -> VmMetric.valueOf(rs.getString("name")),
                                  rs -> rs.getLong("metric_type_id")));
    }

    @Override
    public Long getMetricTypeId(String metric, int osTypeId) {
        return Sql.with(dataSource)
                .exec("SELECT metric_type_id, name FROM metric_type mt JOIN metric m ON mt.metric_id = m.id WHERE m.name = ? AND mt.os_type_id = ?",
                        Sql.nextOrNull(rs -> rs.getLong("metric_type_id")), metric, osTypeId);
    }
}
