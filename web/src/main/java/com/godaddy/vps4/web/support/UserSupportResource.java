package com.godaddy.vps4.web.support;

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
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "support" })

@Path("/api/support")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserSupportResource {
    private static final Logger logger = LoggerFactory.getLogger(UserSupportResource.class);

    private final SupportResource supportResource;
    private final ActionService actionService;
    private final CommandService commandService;
    private final Vps4User supportUser;
    private final String SupportUserName = "Support";

    @Inject
    public UserSupportResource(SupportResource supportResource,
            Vps4UserService vps4UserService,
            ActionService actionService,
            CommandService commandService) {
        this.supportResource = supportResource;
        this.actionService = actionService;
        this.commandService = commandService;
        supportUser = vps4UserService.getUser(SupportUserName);
    }

    @POST
    @Path("vms/{vmId}/addAdminUser")
    public SupportAction addAdminUser(@PathParam("vmId") UUID vmId) {
        logger.info("Adding an admin user to vm " + vmId + " from the support api");
        VirtualMachine vm = supportResource.getVm(vmId);

        String username = "newuser"; // TODO: Generate new username
        String password = "abcdWXYZ1!"; // TODO: Generate a password

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
        
        SupportAction supportAction = new SupportAction(action, command, message.toString());

        return supportAction;
    }

}
