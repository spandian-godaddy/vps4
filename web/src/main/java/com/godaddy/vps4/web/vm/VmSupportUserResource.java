package com.godaddy.vps4.web.vm;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.sysadmin.Vps4AddAdminUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveUser;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.sysadmin.UsernamePasswordGenerator;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmSupportUserResource {
    private static final Logger logger = LoggerFactory.getLogger(VmSupportUserResource.class);

    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;
    private final Vps4User supportUser;
    private final String SupportUserName = "Support";

    @Inject
    public VmSupportUserResource(VmResource vmResource,
            Vps4UserService vps4UserService,
            ActionService actionService,
            CommandService commandService) {
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        supportUser = vps4UserService.getUser(SupportUserName);
    }

    @AdminOnly
    @POST
    @Path("/{vmId}/addAdminUser")
    public ActionWithDetails addAdminUser(@PathParam("vmId") UUID vmId) {
        logger.info("Adding an admin user to vm {} from the support api", vmId);
        VirtualMachine vm = vmResource.getVm(vmId);

        String username = UsernamePasswordGenerator.generateUsername(12);
        String password = UsernamePasswordGenerator.generatePassword(14);

        JSONObject addUserJson = new JSONObject();
        addUserJson.put("username", username);

        long actionId = actionService.createAction(vm.vmId, ActionType.ADD_ADMIN_USER, addUserJson.toJSONString(), supportUser.getId());

        Vps4AddAdminUser.Request addUserRequest = new Vps4AddAdminUser.Request();
        addUserRequest.hfsVmId = vm.hfsVmId;
        addUserRequest.username = username;
        addUserRequest.password = password;
        addUserRequest.actionId = actionId;
        addUserRequest.vmId = vm.vmId;

        CommandState command = Commands.execute(commandService, actionService, "Vps4AddAdminUser", addUserRequest);
        Action action = actionService.getAction(actionId);
        JSONObject message = new JSONObject();
        message.put("Username", username);
        message.put("Password", password);

        return new ActionWithDetails(action, command, message.toString());
    }

    @AdminOnly
    @POST
    @Path("/{vmId}/removeAdminUser/{username}")
    public ActionWithDetails removeAdminUser(@PathParam("vmId") UUID vmId, @PathParam("username") String username) {
        logger.info("Removing user {} from vm {}", username, vmId);
        VirtualMachine vm = vmResource.getVm(vmId);

        JSONObject removeUserJson = new JSONObject();
        removeUserJson.put("username", username);

        long actionId = actionService.createAction(vmId, ActionType.DELETE_ADMIN_USER, removeUserJson.toJSONString(), supportUser.getId());

        Vps4RemoveUser.Request request = new Vps4RemoveUser.Request();
        request.hfsVmId = vm.hfsVmId;
        request.username = username;
        request.actionId = actionId;
        request.vmId = vmId;

        CommandState command = Commands.execute(commandService, actionService, "Vps4RemoveUser", request);

        Action action = actionService.getAction(actionId);

        return new ActionWithDetails(action, command);
    }

}
