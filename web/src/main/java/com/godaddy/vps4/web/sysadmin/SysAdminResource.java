package com.godaddy.vps4.web.sysadmin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.vm.ActionStatus;
import com.godaddy.vps4.Vps4Exception;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Action;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.vm.VmResource.CreateVmAction;


import gdg.hfs.vhfs.sysadmin.SysAdminService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SysAdminResource {

    private static final Logger logger = LoggerFactory.getLogger(SysAdminResource.class);

    static final Map<Long, Action> actions = new ConcurrentHashMap<>();
    static final Map<UUID, CreateVmAction> provisionActions = new ConcurrentHashMap<>();

    static final AtomicLong actionIdPool = new AtomicLong();
    static final ExecutorService threadPool = Executors.newCachedThreadPool();

    final SysAdminService sysAdminService;
    final VmUserService userService;

    @Inject
    public SysAdminResource(SysAdminService sysAdminService, VmUserService userService) {
        this.sysAdminService = sysAdminService;
        this.userService = userService;
    }
    
    public static class UpdatePasswordRequest{
        public String username;
        public String password;
    }
    
    @POST
    @Path("/{vmId}/setPassword")
    public SetPasswordAction setPassword(@QueryParam("vmId") long vmId, UpdatePasswordRequest updatePasswordRequest){
        // TODO: This will need more logic when we include Windows
        // Windows does not have a "root" user.
        String[] usernames = {updatePasswordRequest.username, "root"};
        SetPasswordAction action = new SetPasswordAction(vmId, usernames, updatePasswordRequest.password);
        if (!userService.userExists(updatePasswordRequest.username, vmId)) {
            action.message = "Cannot find user " + action.getUsername() + " for vm "+action.getVmId();
            action.status = ActionStatus.INVALID;
            return action;
        }
        actions.put(action.actionId, action);
        SetPasswordWorker worker = new SetPasswordWorker(sysAdminService, action);

        action.status = ActionStatus.IN_PROGRESS;
        threadPool.execute(() -> {
            try {
                worker.run();
            }
            catch (Vps4Exception e) {
                action.status = ActionStatus.ERROR;
            }
        });
        return action;
    }
    
    public static class SetAdminRequest {
        public String username;
    }
    

    
    @POST
    @Path("/{vmId}/enableAdmin")
    public SetAdminAction enableUserAdmin(@QueryParam("vmId") long vmId, SetAdminRequest setAdminRequest) {
        SetAdminAction action = new SetAdminAction(setAdminRequest.username, vmId, true);
        setUserAdmin(action);
        return action;
    }

    @POST
    @Path("/{vmId}/disableAdmin")
    public SetAdminAction disableUserAdmin(@QueryParam("vmId") long vmId, SetAdminRequest setAdminRequest) {
        SetAdminAction action = new SetAdminAction(setAdminRequest.username, vmId, false);
        setUserAdmin(action);
        return action;
    }
    
    private void setUserAdmin(SetAdminAction action) {
        if (!userService.userExists(action.getUsername(), action.getVmId())) {
            action.message = "Cannot find user " + action.getUsername() + " for vm "+action.getVmId();
            action.status = ActionStatus.INVALID;
            return;
        }
        actions.put(action.actionId, action);
        ToggleAdminWorker worker = new ToggleAdminWorker(sysAdminService, userService, action);

        action.status = ActionStatus.IN_PROGRESS;
        threadPool.execute(() -> {
            try {
                worker.run();
            }
            catch (Vps4Exception e) {
                action.status = ActionStatus.ERROR;
            }
        });

    }
}