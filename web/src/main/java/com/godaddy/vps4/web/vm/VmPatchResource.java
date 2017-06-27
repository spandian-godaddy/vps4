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
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.web.PATCH;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
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
    final Vps4UserService userService;
    final GDUser user;

    @Inject
    public VmPatchResource(VirtualMachineService virtualMachineService,
                           GDUser user,
                           PrivilegeService privilegeService,
                           ActionService actionService,
                           Vps4UserService userService) {
        this.virtualMachineService = virtualMachineService;
        this.privilegeService = privilegeService;
        this.actionService = actionService;
        this.user = user;
        this.userService = userService;
    }

    private void verifyUserPrivilege(UUID vmId) {
        Vps4User vps4User = userService.getOrCreateUserForShopper(user.getShopperId());
        privilegeService.requireAnyPrivilegeToVmId(vps4User, vmId);
    }

    public static class VmPatch {
        public String name;
    }

    @PATCH
    @Path("/{vmId}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Update VM Attributes", httpMethod = "PATCH")
    public Action updateVm(@PathParam("vmId") UUID vmId, VmPatch vmPatch) {
        if (user.isShopper())
            verifyUserPrivilege(vmId);

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
        this.actionService.completeAction(actionId, new JSONObject().toJSONString(), notes.toString());
        return this.actionService.getAction(actionId);
    }
}
