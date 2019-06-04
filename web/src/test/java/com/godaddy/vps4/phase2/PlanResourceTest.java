package com.godaddy.vps4.phase2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.jdbc.DatabaseModule;
import com.godaddy.vps4.plan.Plan;
import com.godaddy.vps4.plan.PlanModule;
import com.godaddy.vps4.vm.Image.ControlPanel;
import com.godaddy.vps4.web.plan.PlanResource;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class PlanResourceTest {

    private Injector injector = Guice.createInjector(
            new DatabaseModule(),
            new PlanModule());

    @Before
    public void setUp() {
        injector.injectMembers(this);
    }

    private PlanResource getPlanResource() {
        return injector.getInstance(PlanResource.class);
    }

    @Test
    public void testGetPlan1066868() {
        int pfid = 1066868;  // tier1, linux, 3mo
        String expectedPackageId = "vps4_linux_tier1_003mo";
        int expectedCpus = 1;
        int expectedMem = 2048;
        int expectedDisk = 40;
        Plan plan = getPlanResource().getPlan(pfid);

        assertEquals(pfid, plan.pfid);
        assertEquals(expectedPackageId, plan.packageId);
        assertEquals(expectedCpus, plan.cpuCoreCount);
        assertEquals(expectedMem, plan.memoryMib);
        assertEquals(expectedDisk, plan.diskGib);
        assertEquals(ControlPanel.MYH, plan.controlPanel);
    }

    @Test
    public void testGetPlan1215752() {
        int pfid = 1215752; // tier1, linux, 3mo
        String expectedPackageId = "vps4_managed_lin_cpanel_tier2_012mo";
        int expectedCpus = 2;
        int expectedMem = 4096;
        int expectedDisk = 60;
        Plan plan = getPlanResource().getPlan(pfid);

        assertEquals(pfid, plan.pfid);
        assertEquals(expectedPackageId, plan.packageId);
        assertEquals(expectedCpus, plan.cpuCoreCount);
        assertEquals(expectedMem, plan.memoryMib);
        assertEquals(expectedDisk, plan.diskGib);
        assertEquals(ControlPanel.CPANEL, plan.controlPanel);
    }

    @Test
    public void testGetPlanListContainsNoNullPackageIds() {
        List<Plan> planList = getPlanResource().getPlanList();
        List<String> packageIds = planList.stream().map(p -> p.packageId).collect(Collectors.toList());

        assertFalse(packageIds.contains(null));
    }

    @Test
    public void testGetUpgradeListPlan1066873() {
        int pfid = 1066873;  // tier2, linux, 6mo
        List<Plan> upgradeList = getPlanResource().getUpgradeList(pfid);
        
        assertEquals(4, upgradeList.size());
        assertEquals("vps4_linux_tier3_006mo", upgradeList.get(0).packageId);
        assertEquals("vps4_linux_tier4_006mo", upgradeList.get(1).packageId);
        assertEquals("vps4_self_managed_high_mem_lin_tier2_006mo", upgradeList.get(2).packageId);
        assertEquals("vps4_self_managed_high_mem_lin_tier4_006mo", upgradeList.get(3).packageId);
    }

    @Test
    public void testGetUpgradeListPlan1215752() {
        int pfid = 1215752; // tier2, linux, 6mo
        List<Plan> upgradeList = getPlanResource().getUpgradeList(pfid);

        assertEquals(4, upgradeList.size());
    }

    @Test
    public void testGetUpgradeListTopTierNoUpgrades() {
        int pfid = 1193702;  // tier4, linux, 12mo, high_mem
        List<Plan> upgradeList = getPlanResource().getUpgradeList(pfid);

        // already highest at highest tier available
        assertTrue(upgradeList.isEmpty());
    }

}
