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

import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
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

    final VirtualMachineService virtualMachineService;
    final PrivilegeService privilegeService;
    final ActionService actionService;
    final Vps4User user;

    @Inject
    public VmPatchResource(VirtualMachineService virtualMachineService,
                           Vps4User user,
                           PrivilegeService privilegeService,
                           ActionService actionService) {
        this.virtualMachineService = virtualMachineService;
        this.privilegeService = privilegeService;
        this.actionService = actionService;
        this.user = user;
    }

    public static class VmPatch {
        public String name;
    }

    @PATCH
    @Path("/{vmId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Update VM Attributes", httpMethod = "PATCH")
    public Action updateVm(@PathParam("vmId") UUID vmId, VmPatch vmPatch) {
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        Map<String, Object> vmPatchMap = new HashMap<>();
        StringBuilder notes = new StringBuilder();
        if (vmPatch.name != null && !vmPatch.name.equals("")){
            vmPatchMap.put("name", vmPatch.name);
            notes.append("Name = " + vmPatch.name);
        }
        logger.info("Updating vm {}'s with {} ", vmId, vmPatchMap.toString());
        long actionId = this.actionService.createAction(vmId, ActionType.UPDATE_SERVER, new JSONObject().toJSONString(), user.getId());
        virtualMachineService.updateVirtualMachine(vmId, vmPatchMap);
        this.actionService.completeAction(actionId, new JSONObject().toJSONString(), notes.toString());
        return this.actionService.getAction(actionId);
    }
}
