package com.godaddy.vps4.plan;

import java.util.List;

public interface PlanService {

    Plan getPlan(int pfid);

    List<Plan> getPlanList();

    List<Plan> getUpgradeList(int pfid);

}
