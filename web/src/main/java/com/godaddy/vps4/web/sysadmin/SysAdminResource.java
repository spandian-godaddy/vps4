package com.godaddy.vps4.web.sysadmin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import com.godaddy.vps4.orchestration.hfs.sysadmin.SetHostname;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetHostname;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4ToggleAdmin;
import com.godaddy.vps4.security.PrivilegeService;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.util.validators.Validator;
import com.godaddy.vps4.util.validators.ValidatorRegistry;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.util.Commands;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
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
    final VirtualMachineService vmService;
    final Vps4User user;

    @Inject
    public SysAdminResource(VmUserService userService,
                            ActionService actionService, 
                            CommandService commandService, 
                            Vps4User user,
                            PrivilegeService privilegeService,
                            VirtualMachineService vmService) {
        this.userService = userService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.privilegeService = privilegeService;
        this.vmService = vmService;
    }
    
    private VirtualMachine getVm(UUID vmId) {
        return vmService.getVirtualMachine(vmId);
    }

    public static class UpdatePasswordRequest{
        public String username;
        public String password;
    }

    @POST
    @Path("/{vmId}/setPassword")
    public Action setPassword(@PathParam("vmId") UUID vmId, UpdatePasswordRequest updatePasswordRequest) {

        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        // TODO: This will need more logic when we include Windows
        VirtualMachine vm = getVm(vmId);
        List<String> usernames = new ArrayList<String>();
        usernames.add(updatePasswordRequest.username);
        if(vm.image.operatingSystem == OperatingSystem.LINUX){
            usernames.add("root");
        }
        
//        SetPasswordAction action = new SetPasswordAction(vmId, usernames, updatePasswordRequest.password);
        if (!userService.userExists(updatePasswordRequest.username, vm.vmId)) {

            // FIXME throw exception
            //action.message = "Cannot find user " + updatePasswordRequest.username + " for vm "+vmId;
            //action.status = ActionStatus.INVALID;
            //return action;
        }
        
        JSONObject pwRequest = new JSONObject();
        pwRequest.put("username", updatePasswordRequest.username);

        long actionId = actionService.createAction(vm.vmId, ActionType.SET_PASSWORD, 
                pwRequest.toJSONString(), user.getId());

        SetPassword.Request request = new SetPassword.Request();
        request.usernames = usernames;
        request.password = updatePasswordRequest.password;
        request.hfsVmId = vm.hfsVmId;

        Vps4SetPassword.Request vps4Request = new Vps4SetPassword.Request();
        vps4Request.actionId = actionId;
        vps4Request.setPasswordRequest = request;

        Commands.execute(commandService, actionService, "Vps4SetPassword", vps4Request);

        return actionService.getAction(actionId);
    }
    
    public static class SetHostnameRequest{
        public String hostname;
    }
    
    @POST
    @Path("/{vmId}/setHostname")
    public Action setHostname(@PathParam("vmId") UUID vmId, SetHostnameRequest setHostnameRequest) {
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        JSONObject hostnameJsonRequest = new JSONObject();
        
        Validator validator = ValidatorRegistry.getInstance().get("hostname");
        if (!validator.isValid(setHostnameRequest.hostname)){
            throw new Vps4Exception("INVALID_HOSTNAME", String.format("%s is an invalid hostname", setHostnameRequest.hostname));
        }
        
        hostnameJsonRequest.put("hostname", setHostnameRequest.hostname);
        
        long actionId = actionService.createAction(vmId, ActionType.SET_HOSTNAME, 
                hostnameJsonRequest.toJSONString(), user.getId());
        
        VirtualMachine vm = getVm(vmId);
        
        SetHostname.Request hfsRequest = new SetHostname.Request();
        hfsRequest.hfsVmId = vm.hfsVmId;
        hfsRequest.hostname = setHostnameRequest.hostname;
        hfsRequest.controlPanel = vm.image.controlPanel.toString().toLowerCase();
        
        Vps4SetHostname.Request vps4Request = new Vps4SetHostname.Request();
        vps4Request.setHostnameRequest = hfsRequest;
        vps4Request.vmId = vmId;
        vps4Request.oldHostname = vm.hostname;
        vps4Request.actionId = actionId;
        
        Commands.execute(commandService, actionService, "Vps4SetHostname", vps4Request);
        
        return actionService.getAction(actionId);
        
    }

    public static class SetAdminRequest {
        public String username;
    }

    @POST
    @Path("/{vmId}/enableAdmin")
    public Action enableUserAdmin(@PathParam("vmId") UUID vmId, SetAdminRequest setAdminRequest) {
        return setUserAdmin(setAdminRequest.username, vmId, true);
    }

    @POST
    @Path("/{vmId}/disableAdmin")
    public Action disableUserAdmin(@PathParam("vmId") UUID vmId, SetAdminRequest setAdminRequest) {
        return setUserAdmin(setAdminRequest.username, vmId, false);
    }

    private Action setUserAdmin(String username, UUID vmId, boolean adminEnabled) {
        logger.info("Username: {} VMid: {} Admin access enabled? {}", username, vmId, adminEnabled);
        privilegeService.requireAnyPrivilegeToVmId(user, vmId);
        VirtualMachine vm = getVm(vmId);
        if (!userService.userExists(username, vm.vmId)) {
            throw new Vps4Exception("VM_USER_NOT_FOUND", "User not found on virtual machine.");
        }
        JSONObject adminRequest = new JSONObject();
        adminRequest.put("username", username);
        adminRequest.put("enabled", adminEnabled);
        long actionId = actionService.createAction(vm.vmId,
                adminEnabled ? ActionType.ENABLE_ADMIN_ACCESS : ActionType.DISABLE_ADMIN_ACCESS,
                adminRequest.toJSONString(), user.getId());


        Vps4ToggleAdmin.Request vps4Request = new Vps4ToggleAdmin.Request();
        vps4Request.actionId = actionId;
        vps4Request.enabled = adminEnabled;
        vps4Request.hfsVmId = vm.hfsVmId;
        vps4Request.vmId = vm.vmId;
        vps4Request.username = username;

        Commands.execute(commandService, actionService, "Vps4ToggleAdmin", vps4Request);
        
        return actionService.getAction(actionId);

    }
}