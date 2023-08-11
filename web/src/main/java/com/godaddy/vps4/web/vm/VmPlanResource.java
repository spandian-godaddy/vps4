package com.godaddy.vps4.web.vm;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.NotFoundException;

import com.godaddy.vps4.plan.Plan;
import com.godaddy.vps4.plan.PlanService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;

import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = {"vms"})
@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmPlanResource {
    private final VmResource vmResource;
    private final PlanService planService;

    @Inject
    public VmPlanResource(VmResource vmResource, PlanService planService) {
        this.vmResource = vmResource;
        this.planService = planService;
    }

    @GET
    @Path("/{vmId}/plans")
    public List<Plan> getPlansForVm(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation
        return planService.getAdjacentPlanList(vm);
    }

    @GET
    @Path("/{vmId}/currentPlan")
    public Plan getCurrentPlan(@PathParam("vmId") UUID vmId, @QueryParam("termMonths") Integer termMonths) {
        VirtualMachine vm = vmResource.getVm(vmId); // auth validation
        Plan currentPlan = planService.getCurrentPlan(vm, termMonths);
        if (currentPlan == null) {
            throw new NotFoundException("No plan found for VM " + vm.vmId);
        }
        return currentPlan;
    }
}
