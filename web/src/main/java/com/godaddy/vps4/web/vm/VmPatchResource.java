package com.godaddy.vps4.web.vm;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmPatchResource {

    private static final Logger logger = LoggerFactory.getLogger(VmPatchResource.class);

    private final VirtualMachineService virtualMachineService;
    private final ActionService actionService;
    private final CreditService creditService;
    private final VmResource vmResource;

    @Inject
    public VmPatchResource(VirtualMachineService virtualMachineService,
                           ActionService actionService,
                           CreditService creditService,
                           VmResource vmResource) {
        this.virtualMachineService = virtualMachineService;
        this.actionService = actionService;
        this.creditService = creditService;
        this.vmResource = vmResource;
    }

    public static class VmPatch {
        public String name;
    }

    @PATCH
    @Path("/{vmId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Update VM Attributes", httpMethod = "PATCH")
    public VmAction updateVm(@PathParam("vmId") UUID vmId, VmPatch vmPatch) {
        VirtualMachine vm = vmResource.getVm(vmId);

        Map<String, Object> vmPatchMap = new HashMap<>();
        StringBuilder notes = new StringBuilder();
        if (vmPatch.name != null && !vmPatch.name.equals("")){
            vmPatchMap.put("name", vmPatch.name);
            notes.append("Name = " + vmPatch.name);
        }
        logger.info("Updating vm {}'s with {} ", vmId, vmPatchMap.toString());

        long vps4UserId = virtualMachineService.getUserIdByVmId(vmId);
        long actionId = this.actionService.createAction(vmId, ActionType.UPDATE_SERVER, new JSONObject().toJSONString(), vps4UserId);
        virtualMachineService.updateVirtualMachine(vmId, vmPatchMap);
        creditService.setCommonName(vm.orionGuid, vmPatch.name);
        this.actionService.completeAction(actionId, new JSONObject().toJSONString(), notes.toString());
        return new VmAction(this.actionService.getAction(actionId));
    }
}
