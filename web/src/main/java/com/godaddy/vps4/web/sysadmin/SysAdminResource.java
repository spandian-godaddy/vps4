package com.godaddy.vps4.web.sysadmin;

import java.util.Arrays;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4ToggleAdmin;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.util.Commands;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SysAdminResource {

    private static final Logger logger = LoggerFactory.getLogger(SysAdminResource.class);

    final VmUserService userService;

    final ActionService actionService;
    final CommandService commandService;
    final PrivilegeService privilegeService;
    final Vps4User user;

    @Inject
    public SysAdminResource(VmUserService userService,
                            ActionService actionService, 
                            CommandService commandService, 
                            Vps4User user,
                            PrivilegeService privilegeService) {
        this.userService = userService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.privilegeService = privilegeService;
    }

    public static class UpdatePasswordRequest{
        public String username;
        public String password;
    }

    @POST
    @Path("/{vmId}/setPassword")
    public Action setPassword(@PathParam("vmId") long vmId, UpdatePasswordRequest updatePasswordRequest) {

        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        // TODO: This will need more logic when we include Windows
        // Windows does not have a "root" user.
        String[] usernames = {updatePasswordRequest.username, "root"};

//        SetPasswordAction action = new SetPasswordAction(vmId, usernames, updatePasswordRequest.password);
        if (!userService.userExists(updatePasswordRequest.username, vmId)) {

            // FIXME throw exception
            //action.message = "Cannot find user " + updatePasswordRequest.username + " for vm "+vmId;
            //action.status = ActionStatus.INVALID;
            //return action;
        }
        
        JSONObject pwRequest = new JSONObject();
        pwRequest.put("username", updatePasswordRequest.username);

        long actionId = actionService.createAction(vmId, ActionType.SET_PASSWORD, 
                pwRequest.toJSONString(), user.getId());

        SetPassword.Request request = new SetPassword.Request();
        request.usernames = Arrays.asList(usernames);
        request.password = updatePasswordRequest.password;
        request.vmId = vmId;

        Vps4SetPassword.Request vps4Request = new Vps4SetPassword.Request();
        vps4Request.actionId = actionId;
        vps4Request.setPasswordRequest = request;

        Commands.execute(commandService, "Vps4SetPassword", vps4Request);

        return actionService.getAction(actionId);
    }

    public static class SetAdminRequest {
        public String username;
    }

    @POST
    @Path("/{vmId}/enableAdmin")
    public Action enableUserAdmin(@PathParam("vmId") long vmId, SetAdminRequest setAdminRequest) {
        return setUserAdmin(setAdminRequest.username, vmId, true);
    }

    @POST
    @Path("/{vmId}/disableAdmin")
    public Action disableUserAdmin(@PathParam("vmId") long vmId, SetAdminRequest setAdminRequest) {
        return setUserAdmin(setAdminRequest.username, vmId, false);
    }

    private Action setUserAdmin(String username, long vmId, boolean adminEnabled) {
        logger.info("Username: {} VMid: {} Admin access enabled? {}", username, vmId, adminEnabled);
        if (!userService.userExists(username, vmId)) {
            throw new Vps4Exception("VM_USER_NOT_FOUND", "User not found on virtual machine.");
        }
        
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);

        JSONObject adminRequest = new JSONObject();
        adminRequest.put("username", username);
        adminRequest.put("enabled", adminEnabled);
        
        long actionId = actionService.createAction(vmId,
                adminEnabled ? ActionType.ENABLE_ADMIN_ACCESS : ActionType.DISABLE_ADMIN_ACCESS,
                adminRequest.toJSONString(), user.getId());


        Vps4ToggleAdmin.Request vps4Request = new Vps4ToggleAdmin.Request();
        vps4Request.actionId = actionId;
        vps4Request.enabled = adminEnabled;
        vps4Request.vmId = vmId;
        vps4Request.username = username;

        Commands.execute(commandService, "Vps4ToggleAdmin", vps4Request);

        return actionService.getAction(actionId);

    }
}