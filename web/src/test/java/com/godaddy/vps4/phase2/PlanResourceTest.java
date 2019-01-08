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
    public void testGetPlan() {
        int pfid = 1066868;  // tier1, linux, 3mo
        String expectedPackageId = "vps4_linux_tier1_003mo";
        Plan plan = getPlanResource().getPlan(pfid);

        assertEquals(pfid, plan.pfid);
        assertEquals(expectedPackageId, plan.packageId);
    }

    @Test
    public void testGetPlanListContainsNoNullPackageIds() {
        List<Plan> planList = getPlanResource().getPlanList();
        List<String> packageIds = planList.stream().map(p -> p.packageId).collect(Collectors.toList());

        assertFalse(packageIds.contains(null));
    }

    @Test
    public void testGetUpgradeList() {
        int pfid = 1066873;  // tier2, linux, 6mo
        List<Plan> upgradeList = getPlanResource().getUpgradeList(pfid);

        // Only tier3 and tier4 available, tier1 not in upgrade list
        assertEquals(2, upgradeList.size());
        assertEquals("vps4_linux_tier3_006mo", upgradeList.get(0).packageId);
        assertEquals("vps4_linux_tier4_006mo", upgradeList.get(1).packageId);
    }

    @Test
    public void testGetUpgradeListTopTierNoUpgrades() {
        int pfid = 1066910;  // tier4, linux, 12mo
        List<Plan> upgradeList = getPlanResource().getUpgradeList(pfid);

        // already highest at highest tier available
        assertTrue(upgradeList.isEmpty());
    }

}
