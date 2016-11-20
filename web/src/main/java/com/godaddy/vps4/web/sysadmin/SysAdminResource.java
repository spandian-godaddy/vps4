package com.godaddy.vps4.web.sysadmin;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;

import gdg.hfs.vhfs.sysadmin.SysAdminService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SysAdminResource {

    private static final Logger logger = LoggerFactory.getLogger(SysAdminResource.class);

    final SysAdminService sysAdminService;
    final VmUserService userService;

    final ActionService actionService;
    final Vps4User user;

    @Inject
    public SysAdminResource(SysAdminService sysAdminService, VmUserService userService,
            ActionService actionService, Vps4User user) {
        this.sysAdminService = sysAdminService;
        this.userService = userService;
        this.actionService = actionService;
        this.user = user;
    }

    public static class UpdatePasswordRequest{
        public String username;
        public String password;
    }

    @POST
    @Path("/{vmId}/setPassword")
    public Action setPassword(@QueryParam("vmId") long vmId, UpdatePasswordRequest updatePasswordRequest){

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

        long actionId = actionService.createAction(vmId, ActionType.SET_PASSWORD, "", user.getId());

        // FIXME orchestration client call to SetPassword

        return actionService.getAction(actionId);
    }

    public static class SetAdminRequest {
        public String username;
    }



    @POST
    @Path("/{vmId}/enableAdmin")
    public Action enableUserAdmin(@QueryParam("vmId") long vmId, SetAdminRequest setAdminRequest) {

        return setUserAdmin(setAdminRequest.username, vmId, true);
    }

    @POST
    @Path("/{vmId}/disableAdmin")
    public Action disableUserAdmin(@QueryParam("vmId") long vmId, SetAdminRequest setAdminRequest) {
        return setUserAdmin(setAdminRequest.username, vmId, false);
    }

    private Action setUserAdmin(String username, long vmId, boolean adminEnabled) {
        if (!userService.userExists(username, vmId)) {
            // FIXME throw exception
            //action.message = "Cannot find user " + action.getUsername() + " for vm "+vmId;
            //action.status = ActionStatus.INVALID;
            //return;
        }
        //actions.put(action.actionId, action);

        long actionId = actionService.createAction(vmId,
                adminEnabled ? ActionType.ENABLE_ADMIN_ACCESS : ActionType.DISABLE_ADMIN_ACCESS,
                "", user.getId());

        // TODO call orchestration client
        //ToggleAdminWorker worker = new ToggleAdminWorker(sysAdminService, userService, action);

//        action.status = ActionStatus.IN_PROGRESS;
//        threadPool.execute(() -> {
//            try {
//                worker.run();
//            }
//            catch (Vps4Exception e) {
//                action.status = ActionStatus.ERROR;
//            }
//        });

        return actionService.getAction(actionId);

    }
}