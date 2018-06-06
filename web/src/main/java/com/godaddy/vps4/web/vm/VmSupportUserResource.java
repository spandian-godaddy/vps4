package com.godaddy.vps4.web.vm;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4AddSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetPassword;
import com.godaddy.vps4.security.Vps4User;
import com.godaddy.vps4.security.Vps4UserService;
import com.godaddy.vps4.sysadmin.UsernamePasswordGenerator;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.*;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.StaffOnly;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
    private final Cryptography cryptography;
    private final Config config;

    @Inject
    public VmSupportUserResource(VmResource vmResource,
                                 Vps4UserService vps4UserService,
                                 ActionService actionService,
                                 CommandService commandService,
                                 VmUserService vmUserService,
                                 Cryptography cryptography,
                                 Config config) {
        this.vmResource = vmResource;
        this.actionService = actionService;
        this.commandService = commandService;
        this.vmUserService = vmUserService;
        this.cryptography = cryptography;
        supportUser = vps4UserService.getUser(SupportUserName);
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    @StaffOnly
    @POST
    @Path("/{vmId}/supportUsers")
    @ApiOperation(value = "Add a support user to a VM")
    public VmActionWithDetails addSupportUsers(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);

        // create username from prefix with max length of 12
        String usernamePrefix = config.get("vps4.supportUser.namePrefix", "support_");
        String username = UsernamePasswordGenerator.generateUsername(usernamePrefix, 12);

        // create password
        String password = UsernamePasswordGenerator.generatePassword(14);

        // create VPS4 add_support_user action
        JSONObject addUserJson = new JSONObject();
        addUserJson.put("username", username);
        long actionId = actionService.createAction(vm.vmId, ActionType.ADD_SUPPORT_USER, addUserJson.toJSONString(), supportUser.getId());

        // execute Vps4AddSupportUser command
        Vps4AddSupportUser.Request addUserRequest = new Vps4AddSupportUser.Request();
        addUserRequest.hfsVmId = vm.hfsVmId;
        addUserRequest.username = username;
        addUserRequest.encryptedPassword = cryptography.encrypt(password);
        addUserRequest.actionId = actionId;
        addUserRequest.vmId = vm.vmId;
        CommandState command = Commands.execute(commandService, actionService, "Vps4AddSupportUser", addUserRequest);

        // log message
        logger.info("Added support user {} to vm {}", username, vmId);

        // return VM action with username and password in message
        Action action = actionService.getAction(actionId);
        JSONObject message = new JSONObject();
        message.put("Username", username);
        message.put("Password", password);
        return new VmActionWithDetails(action, command, message.toString());
    }

    @SuppressWarnings("unchecked")
    @StaffOnly
    @DELETE
    @Path("/{vmId}/supportUsers/{supportUsername}")
    @ApiOperation(value = "Remove a support user from a VM")
    public VmActionWithDetails removeSupportUsers(@PathParam("vmId") UUID vmId, @PathParam("supportUsername") String username) {
        VirtualMachine vm = vmResource.getVm(vmId);

        // validate that username exists
        List<VmUser> users = vmUserService.getSupportUsers(vmId);
        if (users.stream().anyMatch(u -> u.username.equals(username))) {
            // create VPS4 remove_support_user action
            JSONObject removeUserJson = new JSONObject();
            removeUserJson.put("username", username);
            long actionId = actionService.createAction(vmId, ActionType.REMOVE_SUPPORT_USER, removeUserJson.toJSONString(), supportUser.getId());

            // execute Vps4RemoveSupportUser command
            Vps4RemoveSupportUser.Request request = new Vps4RemoveSupportUser.Request();
            request.hfsVmId = vm.hfsVmId;
            request.username = username;
            request.actionId = actionId;
            request.vmId = vmId;
            CommandState command = Commands.execute(commandService, actionService, "Vps4RemoveSupportUser", request);

            // log message
            logger.info("Removed support user {} from vm {}", username, vmId);

            // return VM action with username and password in message
            Action action = actionService.getAction(actionId);
            return new VmActionWithDetails(action, command);
        } else {
            logger.info("Support user {} not found in vm {}", username, vmId);
            throw new NotFoundException("Support user not found");
        }
    }

    /**
     * @deprecated this will be replaced with {@link UserResource#getUsers(java.util.UUID, com.godaddy.vps4.vm.VmUserType)}.
     */
    @Deprecated
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

    /**
     * @deprecated this will be replaced with {@link #addSupportUsers(UUID)}.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    @StaffOnly
    @POST
    @Path("/{vmId}/supportUser")
    public VmActionWithDetails addSupportUser(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);
        VmUser user = vmUserService.getSupportUser(vmId);
        String password = UsernamePasswordGenerator.generatePassword(14);

        if (user == null) {
            logger.info("Adding an admin user to vm {} from the support api", vmId);
            String usernamePrefix = config.get("vps4.supportUser.namePrefix", "support_");
            String username = UsernamePasswordGenerator.generateUsername(usernamePrefix, 12);

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

            List<String> usernames = new ArrayList<>();
            usernames.add(username);

            SetPassword.Request request = new SetPassword.Request();
            request.usernames = usernames;
            request.encryptedPassword = cryptography.encrypt(password);
            request.hfsVmId = vm.hfsVmId;

            Vps4SetPassword.Request vps4Request = new Vps4SetPassword.Request();
            vps4Request.actionId = actionId;
            vps4Request.vmId = vm.vmId;
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

    /**
     * @deprecated this will be replaced with {@link #removeSupportUsers(UUID, String)}.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    @StaffOnly
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
