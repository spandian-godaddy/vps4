package com.godaddy.vps4.plan.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.sql.DataSource;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.plan.Plan;
import com.godaddy.vps4.plan.PlanService;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;

public class JdbcPlanService implements PlanService {

    private static List<Plan> plans;
    private static Map<Integer, Plan> planMap;
    private final DataSource dataSource;
    private String selectPlanQuery = "SELECT p.pfid, p.package_id, p.os_type_id, p.term_months, p.control_panel_id, "
                + "s.cpu_core_count, s.memory_mib, s.disk_gib, s.tier "
                + "FROM plan p "
                + "JOIN virtual_machine_spec s ON s.spec_id = p.spec_id "
                + "JOIN server_type m ON m.server_type_id = s.server_type_id "
                + "JOIN os_type os ON os.os_type_id = p.os_type_id ";


    @Inject
    public JdbcPlanService(DataSource dataSource) {
        this.dataSource = dataSource;
        initializePlansListAndMap();
    }

    private void initializePlansListAndMap() {
        if (plans == null) {
            plans = getPlanListFromDatabase();
            planMap = plans.stream().collect(Collectors.toMap(p -> p.pfid, p -> p));
        }
    }

    private List<Plan> getPlanListFromDatabase() {
        return Sql.with(dataSource).exec(selectPlanQuery
                + "WHERE p.package_id IS NOT NULL ;",  // hide plans not yet available for sale
                Sql.listOf(this::mapPlan));
    }

    @Override
    public Plan getPlan(int pfid) {
        return planMap.get(pfid);
    }

    private Plan mapPlan(ResultSet rs) throws SQLException {
        Plan plan = new Plan();
        plan.cpuCoreCount = rs.getInt("cpu_core_count");
        plan.memoryMib = rs.getInt("memory_mib");
        plan.diskGib = rs.getInt("disk_gib");
        plan.pfid = rs.getInt("pfid");
        plan.packageId = rs.getString("package_id");
        plan.os = OperatingSystem.valueOf(rs.getInt("os_type_id"));
        plan.termMonths = rs.getInt("term_months");
        plan.tier = rs.getInt("tier");
        plan.controlPanel = ControlPanel.valueOf(rs.getInt("control_panel_id"));
        return plan;
    }

    @Override
    public List<Plan> getUpgradeList(int pfid) {
        Plan originalPlan = planMap.get(pfid);
        return plans.stream()
                .filter(p -> p.os == originalPlan.os 
                        && p.termMonths == originalPlan.termMonths 
                        && p.controlPanel == originalPlan.controlPanel
                        && p.tier > originalPlan.tier)
                .collect(Collectors.toList());
    }

    @Override
    public List<Plan> getPlanList() {
        return plans;
    }
}
