package com.godaddy.vps4.web.plan;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.godaddy.vps4.plan.Plan;
import com.godaddy.vps4.plan.PlanService;

public class PlanResourceTest {
    
    private PlanService planService = mock(PlanService.class);
    private PlanResource resource;
    private Plan mockPlan;

    @Before
    public void prepareTests() {
        mockPlan = mock(Plan.class);
        resource = new PlanResource(planService);
    }
    
    @Test
    public void testGetPlan() {
        when(planService.getPlan(mockPlan.pfid)).thenReturn(mockPlan);
        Plan plan = resource.getPlan(mockPlan.pfid);
        assertEquals(mockPlan, plan);
    }

    @Test
    public void testGetPlanList() {
        List<Plan> testPlanList = new ArrayList<Plan>();
        testPlanList.add(mockPlan);
        testPlanList.add(mock(Plan.class));
        testPlanList.add(mock(Plan.class));
        when(planService.getPlanList()).thenReturn(testPlanList);

        List<Plan> planList = resource.getPlanList();

        assertEquals(testPlanList, planList);
    }

    @Test
    public void testGetPlanUpgradeList() {
        List<Plan> testPlanList = new ArrayList<Plan>();
        testPlanList.add(mockPlan);
        testPlanList.add(mock(Plan.class));
        testPlanList.add(mock(Plan.class));
        when(planService.getUpgradeList(123)).thenReturn(testPlanList);

        List<Plan> planList = resource.getUpgradeList(123);

        assertEquals(testPlanList, planList);
    }
}
