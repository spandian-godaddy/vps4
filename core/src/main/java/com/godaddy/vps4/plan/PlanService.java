package com.godaddy.vps4.plan;

import java.util.List;

import com.godaddy.vps4.vm.VirtualMachine;

public interface PlanService {

    Plan getPlan(int pfid);

    Plan getCurrentPlan(VirtualMachine vm, int termMonths);

    List<Plan> getPlanList();

    List<Plan> getAdjacentPlanList(VirtualMachine vm);

    List<Plan> getUpgradeList(int pfid);

}
