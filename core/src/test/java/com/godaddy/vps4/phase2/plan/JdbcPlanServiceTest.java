package com.godaddy.vps4.phase2.plan;

import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.godaddy.hfs.jdbc.Sql;
import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.plan.Plan;
import com.godaddy.vps4.plan.PlanService;
import com.godaddy.vps4.plan.jdbc.JdbcPlanService;
import com.godaddy.vps4.vm.Image;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.ServerSpec;
import com.godaddy.vps4.vm.VirtualMachine;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.junit.Assert.*;

public class JdbcPlanServiceTest {

    Injector injector = Guice.createInjector(new DatabaseModule());
    DataSource dataSource = injector.getInstance(DataSource.class);
    PlanService planService;
    int[] pfids = { 123, 234, 345, 456, 567, 678, 789, 890};


    @Before
    public void prepareTest() {
        String insertQuery = "INSERT INTO plan (pfid, package_id, term_months, os_type_id, spec_id, control_panel_id, enabled)"
                + " VALUES (?,?,?,?,"
                + " (select distinct spec_id from virtual_machine_spec"
                + "  where tier = ? and server_type_id = 1 and valid_until = 'infinity'),"
                + " ?,?)";
        Sql.with(dataSource).exec(insertQuery, null, 123, "test_plan_tier_1", 12, 1, 10, 1, true);
        Sql.with(dataSource).exec(insertQuery, null, 234, "test_plan_tier_2", 12, 1, 20, 1, true);
        Sql.with(dataSource).exec(insertQuery, null, 345, "test_plan_tier_3", 12, 1, 30, 1, false);
        Sql.with(dataSource).exec(insertQuery, null, 456, "test_plan_tier_4", 12, 1, 40, 1, true);
        Sql.with(dataSource).exec(insertQuery, null, 567, "test_plan_tier_3_disabled", 1, 1, 30, 1, false);
        Sql.with(dataSource).exec(insertQuery, null, 678, "test_plan_tier_4_disabled", 1, 1, 40, 1, false);
        Sql.with(dataSource).exec(insertQuery, null, 789, "test_plan_tier_3_windows", 1, 2, 30, 1, true);
        Sql.with(dataSource).exec(insertQuery, null, 890, "test_plan_tier_4_myh", 1, 1, 40, 0, true);
        planService = new JdbcPlanService(dataSource);
    }

    @After
    public void cleanup() {
        for (int i : pfids) {
            Sql.with(dataSource).exec("DELETE FROM plan WHERE pfid=?", null, i);
        }
    }

    @Test
    public void testGetPlan123() {
        Plan plan = planService.getPlan(123);

        assertEquals("test_plan_tier_1", plan.packageId);
        assertEquals(12, plan.termMonths);
        assertEquals(OperatingSystem.LINUX, plan.os);
        assertEquals(10, plan.tier);
        assertEquals(ControlPanel.CPANEL, plan.controlPanel);
    }

    @Test
    public void verifyCanGetDisabledPlan() {
        Plan plan = planService.getPlan(345);

        assertEquals("test_plan_tier_3", plan.packageId);
        assertEquals(12, plan.termMonths);
        assertEquals(OperatingSystem.LINUX, plan.os);
        assertEquals(30, plan.tier);
        assertEquals(ControlPanel.CPANEL, plan.controlPanel);
    }

    @Test
    public void testGetPlanList() {
        int[] activePfids = { 123, 234, 456, 789, 890 };

        List<Plan> planList = planService.getPlanList();

        List<Integer> planListPfids = planList.stream().map(x -> x.pfid).collect(Collectors.toList());
        for (int pfid : activePfids) {
            assertTrue(planListPfids.contains(pfid));
        }
    }

    @Test
    public void testGetUpgradeList123() {
        int[] upgradePfids = {234, 456};
        int[] excludePfis = { 123, 345, 567, 678, 789, 890 };

        List<Plan> planList = planService.getUpgradeList(123);

        List<Integer> planListPfids = planList.stream().map(x -> x.pfid).collect(Collectors.toList());
        for (int pfid : upgradePfids) {
            assertTrue(planListPfids.contains(pfid));
        }
        for (int pfid : excludePfis) {
            assertFalse(planListPfids.contains(pfid));
        }
    }

    @Test
    public void verifyDisabledPlansCanStillUpgrade() {
        int upgradePfid = 456;
        List<Plan> planList = planService.getUpgradeList(345);
        List<Integer> planListPfids = planList.stream().map(x -> x.pfid).collect(Collectors.toList());
        assertTrue(planListPfids.contains(upgradePfid));
    }

    @Test
    public void testGetAdjacentPlanList() {
        VirtualMachine vm = new VirtualMachine();
        vm.spec = new ServerSpec();
        vm.spec.tier = 40;
        vm.image = new Image();
        vm.image.operatingSystem = OperatingSystem.LINUX;
        List<String> adjacentPackages = planService.getAdjacentPlanList(vm)
                                                   .stream()
                                                   .map(plan -> plan.packageId)
                                                   .collect(Collectors.toList());
        assertTrue(adjacentPackages.contains("test_plan_tier_4_myh"));
        assertTrue(adjacentPackages.contains("test_plan_tier_4_disabled"));
        assertFalse(adjacentPackages.contains("test_plan_tier_3_windows"));
    }

    @Test
    public void testGetCurrentPlan() {
        VirtualMachine vm = new VirtualMachine();
        vm.spec = new ServerSpec();
        vm.spec.tier = 10;
        vm.image = new Image();
        vm.image.operatingSystem = OperatingSystem.LINUX;
        vm.image.controlPanel = ControlPanel.CPANEL;
        Plan currentPlan = planService.getCurrentPlan(vm, 1);

        assertEquals(1215721, currentPlan.pfid);
        assertEquals("vps4_managed_lin_cpanel_tier1_001mo", currentPlan.packageId);
    }

    @Test
    public void testGetCurrentPlanReturnsNullForUnknownTier() {
        VirtualMachine vm = new VirtualMachine();
        vm.spec = new ServerSpec();
        vm.spec.tier = 11;
        vm.image = new Image();
        vm.image.operatingSystem = OperatingSystem.LINUX;
        vm.image.controlPanel = ControlPanel.CPANEL;
        Plan currentPlan = planService.getCurrentPlan(vm, 1);

        assertNull(currentPlan);
    }
}
