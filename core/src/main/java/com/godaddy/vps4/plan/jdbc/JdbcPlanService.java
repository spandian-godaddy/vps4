package com.godaddy.vps4.plan.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.plan.Plan;
import com.godaddy.vps4.plan.PlanService;

public class JdbcPlanService implements PlanService {

    private final DataSource dataSource;
    private String selectPlanQuery = "SELECT p.pfid, p.package_id, s.cpu_core_count, s.memory_mib, s.disk_gib "
                + "FROM plan p "
                + "JOIN virtual_machine_spec s ON s.spec_id = p.spec_id "
                + "JOIN server_type m ON m.server_type_id = s.server_type_id "
                + "JOIN os_type os ON os.os_type_id = p.os_type_id ";

    @Inject
    public JdbcPlanService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Plan getPlan(int pfid) {
        return Sql.with(dataSource).exec(selectPlanQuery + "WHERE p.pfid = ?; ",
                Sql.nextOrNull(this::mapPlan), pfid);
    }

    Plan mapPlan(ResultSet rs) throws SQLException {
        Plan plan = new Plan();
        plan.cpuCoreCount = rs.getInt("cpu_core_count");
        plan.memoryMib = rs.getInt("memory_mib");
        plan.diskGib = rs.getInt("disk_gib");
        plan.pfid = rs.getInt("pfid");
        plan.packageId = rs.getString("package_id");
        return plan;
    }

    @Override
    public List<Plan> getPlanList() {
        return Sql.with(dataSource).exec(selectPlanQuery
                + "WHERE p.package_id IS NOT NULL ;",  // hide plans not yet available for sale
                Sql.listOf(this::mapPlan));
    }

    @Override
    public List<Plan> getUpgradeList(int pfid) {
        return Sql.with(dataSource).exec(
                "SELECT p1.pfid, p1.package_id, s1.cpu_core_count, s1.memory_mib, s1.disk_gib "
                + "FROM plan p1 "
                + "JOIN virtual_machine_spec s1 ON s1.spec_id = p1.spec_id "
                + "JOIN ( "  // subquery to select plans with same os and term
                    + "SELECT term_months, os_type_id, spec.tier FROM plan "
                    + "JOIN virtual_machine_spec spec ON spec.spec_id = plan.spec_id "
                    + "WHERE plan.pfid = ?) p2 "
                    + "ON (p1.os_type_id = p2.os_type_id AND p1.term_months = p2.term_months) "
                + "WHERE p1.package_id IS NOT NULL "  // ignore plans without a package id (not yet available on FOS)
                + "AND s1.tier > p2.tier "  // upgrades only, cannot downgrade
                + "ORDER BY p1.package_id;",
                Sql.listOf(this::mapPlan), pfid);
    }

}
