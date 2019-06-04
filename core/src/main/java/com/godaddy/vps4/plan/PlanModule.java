package com.godaddy.vps4.plan;

import com.godaddy.vps4.plan.jdbc.JdbcPlanService;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class PlanModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(PlanService.class).to(JdbcPlanService.class).in(Scopes.SINGLETON);
    }
}
