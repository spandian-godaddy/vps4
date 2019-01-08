package com.godaddy.vps4.web.plan;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.plan.Plan;
import com.godaddy.vps4.plan.PlanService;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "plans" })

@Path("/api/plans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlanResource {

    private final PlanService planService;

    @Inject
    public PlanResource(PlanService planService) {
        this.planService = planService;
    }

    @GET
    @Path("/{pfid}")
    public Plan getPlan(@PathParam("pfid") int pfid) {
        return planService.getPlan(pfid);
    }

    @GET
    @Path("/")
    public List<Plan> getPlanList() {
        return planService.getPlanList();
    }

    @GET
    @Path("/{pfid}/upgrades")
    public List<Plan> getUpgradeList(@PathParam("pfid") int pfid) {
        return planService.getUpgradeList(pfid);
    }

}
