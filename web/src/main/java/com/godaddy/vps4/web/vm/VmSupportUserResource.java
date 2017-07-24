package com.godaddy.vps4.web.vm;

import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4AddSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetPassword;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.sysadmin.UsernamePasswordGenerator;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.AdminOnly;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VmSupportUserResource {
    private static final Logger logger = LoggerFactory.getLogger(VmSupportUserResource.class);

    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;
    private final VmUserService vmUserService;
    private final Vps4User supportUser;
    private final String SupportUserName = "Support";

    @Inject
    public VmSupportUserResource(VmResource vmResource,
                                 Vps4UserService vps4UserService,
                                 ActionService actionService,
                                 CommandService commandService,
                                 VmUserService vmUserService) {
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        this.vmUserService = vmUserService;
        supportUser = vps4UserService.getUser(SupportUserName);
    }

    @GET
    @Path("/{vmId}/supportUser")
    public VmUser getSupportUser(@PathParam("vmId") UUID vmId) {
        vmResource.getVm(vmId);
        VmUser user = vmUserService.getSupportUser(vmId);

        if (user == null) {
            logger.info("No support user found in vm {}", vmId);
            throw new NotFoundException("No support user found");
        } else {
            logger.info("Found support user {} on vm {}", user.username, vmId);
            return user;
        }
    }

    @AdminOnly
    @POST
    @Path("/{vmId}/supportUser")
    public ActionWithDetails addSupportUser(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        VmUser user = vmUserService.getSupportUser(vmId);
        String password = UsernamePasswordGenerator.generatePassword(14);

        if (user == null) {
            logger.info("Adding an admin user to vm {} from the support api", vmId);
            String username = UsernamePasswordGenerator.generateUsername(12);

            JSONObject addUserJson = new JSONObject();
            addUserJson.put("username", username);

            long actionId = actionService.createAction(vm.vmId, ActionType.ADD_SUPPORT_USER, addUserJson.toJSONString(), supportUser.getId());

            Vps4AddSupportUser.Request addUserRequest = new Vps4AddSupportUser.Request();
            addUserRequest.hfsVmId = vm.hfsVmId;
            addUserRequest.username = username;
            addUserRequest.password = password;
            addUserRequest.actionId = actionId;
            addUserRequest.vmId = vm.vmId;

            CommandState command = Commands.execute(commandService, actionService, "Vps4AddSupportUser", addUserRequest);
            Action action = actionService.getAction(actionId);
            JSONObject message = new JSONObject();
            message.put("Username", username);
            message.put("Password", password);

            return new ActionWithDetails(action, command, message.toString());
        } else {
            logger.info("Changing password for admin user on vm {} from the support api", vmId);
            String username = user.username;

            JSONObject setPasswordJson = new JSONObject();
            setPasswordJson.put("username", username);

            long actionId = actionService.createAction(vm.vmId, ActionType.SET_PASSWORD, setPasswordJson.toJSONString(), supportUser.getId());

            List<String> usernames = new ArrayList<String>();
            usernames.add(username);

            SetPassword.Request request = new SetPassword.Request();
            request.usernames = usernames;
            request.password = password;
            request.hfsVmId = vm.hfsVmId;

            Vps4SetPassword.Request vps4Request = new Vps4SetPassword.Request();
            vps4Request.actionId = actionId;
            vps4Request.setPasswordRequest = request;
            vps4Request.controlPanel = vm.image.controlPanel;

            CommandState command = Commands.execute(commandService, actionService, "Vps4SetPassword", vps4Request);
            Action action = actionService.getAction(actionId);
            JSONObject message = new JSONObject();
            message.put("Username", username);
            message.put("Password", password);

            return new ActionWithDetails(action, command, message.toString());
        }
    }

    @AdminOnly
    @DELETE
    @Path("/{vmId}/supportUser")
    public ActionWithDetails removeSupportUser(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        VmUser user = vmUserService.getSupportUser(vmId);

        if (user == null) {
            logger.info("No support user found in vm {}", vmId);
            throw new NotFoundException("No support user found");
        } else {
            logger.info("Removing user {} from vm {}", user.username, vmId);

            JSONObject removeUserJson = new JSONObject();
            removeUserJson.put("username", user.username);

            long actionId = actionService.createAction(vmId, ActionType.REMOVE_SUPPORT_USER, removeUserJson.toJSONString(), supportUser.getId());

            Vps4RemoveUser.Request request = new Vps4RemoveUser.Request();
            request.hfsVmId = vm.hfsVmId;
            request.username = user.username;
            request.actionId = actionId;
            request.vmId = vmId;

            CommandState command = Commands.execute(commandService, actionService, "Vps4RemoveUser", request);

            Action action = actionService.getAction(actionId);

            return new ActionWithDetails(action, command);
        }
    }
}
