package com.godaddy.vps4.web.vm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4AddSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetPassword;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.sysadmin.UsernamePasswordGenerator;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmUser;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.EmployeeOnly;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;

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
    private final Cryptography cryptography;

    @Inject
    public VmSupportUserResource(VmResource vmResource,
            Vps4UserService vps4UserService,
            ActionService actionService,
            CommandService commandService,
            VmUserService vmUserService,
            Cryptography cryptography) {
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        this.vmUserService = vmUserService;
        this.cryptography = cryptography;
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
        }

        logger.info("Found support user {} on vm {}", user.username, vmId);
        return user;
    }

    @SuppressWarnings("unchecked")
    @EmployeeOnly
    @POST
    @Path("/{vmId}/supportUser")
    public VmActionWithDetails addSupportUser(@PathParam("vmId") UUID vmId) {
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
            addUserRequest.encryptedPassword = cryptography.encrypt(password);
            addUserRequest.actionId = actionId;
            addUserRequest.vmId = vm.vmId;

            CommandState command = Commands.execute(commandService, actionService, "Vps4AddSupportUser", addUserRequest);
            Action action = actionService.getAction(actionId);
            JSONObject message = new JSONObject();
            message.put("Username", username);
            message.put("Password", password);

            return new VmActionWithDetails(action, command, message.toString());
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
            request.encryptedPassword = cryptography.encrypt(password);
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

            return new VmActionWithDetails(action, command, message.toString());
        }
    }

    @SuppressWarnings("unchecked")
    @EmployeeOnly
    @DELETE
    @Path("/{vmId}/supportUser")
    public VmActionWithDetails removeSupportUser(@PathParam("vmId") UUID vmId) {
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

            Vps4RemoveSupportUser.Request request = new Vps4RemoveSupportUser.Request();
            request.hfsVmId = vm.hfsVmId;
            request.username = user.username;
            request.actionId = actionId;
            request.vmId = vmId;

            CommandState command = Commands.execute(commandService, actionService, "Vps4RemoveSupportUser", request);

            Action action = actionService.getAction(actionId);

            return new VmActionWithDetails(action, command);
        }
    }
}
