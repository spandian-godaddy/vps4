package com.godaddy.vps4.web.vm;

import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.godaddy.vps4.orchestration.sysadmin.Vps4SetSupportUserPassword;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.config.Config;
import com.godaddy.vps4.orchestration.hfs.sysadmin.SetPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4AddSupportUser;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.sysadmin.UsernamePasswordGenerator;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.Action;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.security.RequiresRole;
import com.godaddy.vps4.web.util.Commands;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandService;
import gdg.hfs.orchestration.CommandState;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Vps4Api
@Api(tags = {"vms"})

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresRole(roles = {GDUser.Role.ADMIN, GDUser.Role.HS_LEAD, GDUser.Role.HS_AGENT, GDUser.Role.SUSPEND_AUTH})
public class VmSupportUserResource {
    private static final Logger logger = LoggerFactory.getLogger(VmSupportUserResource.class);

    private final VmResource vmResource;
    private final ActionService actionService;
    private final CommandService commandService;
    private final VmUserService vmUserService;
    private final Cryptography cryptography;
    private final Config config;
    private final GDUser gdUser;

    @Inject
    public VmSupportUserResource(GDUser user, 
                                 VmResource vmResource,
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
        this.config = config;
        this.gdUser = user;
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("/{vmId}/supportUsers")
    @ApiOperation(value = "Add a support user to a VM")
    public VmActionWithDetails addSupportUsers(@PathParam("vmId") UUID vmId) {
        VirtualMachine vm = vmResource.getVm(vmId);

        String usernamePrefix = config.get("vps4.supportUser.namePrefix", "support_");
        String username = UsernamePasswordGenerator.generateUsername(usernamePrefix, 12);

        String password = UsernamePasswordGenerator.generatePassword(14);

        JSONObject addUserJson = new JSONObject();
        addUserJson.put("username", username);
        long actionId = actionService.createAction(vm.vmId, ActionType.ADD_SUPPORT_USER, addUserJson.toJSONString(), gdUser.getUsername());

        Vps4AddSupportUser.Request addUserRequest = new Vps4AddSupportUser.Request();
        addUserRequest.hfsVmId = vm.hfsVmId;
        addUserRequest.username = username;
        addUserRequest.encryptedPassword = cryptography.encrypt(password);
        addUserRequest.actionId = actionId;
        addUserRequest.vmId = vm.vmId;
        CommandState command = Commands.execute(commandService, actionService, "Vps4AddSupportUser", addUserRequest);

        logger.info("Added support user {} to vm {}", username, vmId);

        Action action = actionService.getAction(actionId);
        JSONObject message = new JSONObject();
        message.put("Username", username);
        message.put("Password", password);
        return new VmActionWithDetails(action, command, message.toString(), gdUser.isEmployee());
    }

    @SuppressWarnings("unchecked")
    @DELETE
    @Path("/{vmId}/supportUsers/{supportUsername}")
    @ApiOperation(value = "Remove a support user from a VM")
    public VmActionWithDetails removeSupportUsers(@PathParam("vmId") UUID vmId, @PathParam("supportUsername") String username) {
        VirtualMachine vm = vmResource.getVm(vmId);
        
        if (vmUserService.supportUserExists(username, vmId)) {
            JSONObject removeUserJson = new JSONObject();
            removeUserJson.put("username", username);
            long actionId = actionService.createAction(vmId, ActionType.REMOVE_SUPPORT_USER, removeUserJson.toJSONString(), gdUser.getUsername());

            Vps4RemoveSupportUser.Request request = new Vps4RemoveSupportUser.Request();
            request.hfsVmId = vm.hfsVmId;
            request.username = username;
            request.actionId = actionId;
            request.vmId = vmId;
            CommandState command = Commands.execute(commandService, actionService, "Vps4RemoveSupportUser", request);

            logger.info("Removed support user {} from vm {}", username, vmId);

            Action action = actionService.getAction(actionId);
            return new VmActionWithDetails(action, command, gdUser.isEmployee());
        } else {
            logger.info("Support user {} not found in vm {}. Return successfully", username, vmId);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("/{vmId}/supportUsers/{supportUsername}/changePassword")
    @ApiOperation(value = "Change the password for a support user")
    public VmActionWithDetails changeSupportUsersPassword(@PathParam("vmId") UUID vmId, @PathParam("supportUsername") String username) {
        VirtualMachine vm = vmResource.getVm(vmId);

        if (!vmUserService.supportUserExists(username, vmId)) {
            logger.info("Support user {} not found in vm {}", username, vmId);
            throw new NotFoundException("Support user not found");
        }

        String password = UsernamePasswordGenerator.generatePassword(14);

        JSONObject setPasswordJson = new JSONObject();
        setPasswordJson.put("username", username);
        long actionId = actionService.createAction(vm.vmId, ActionType.SET_SUPPORT_USER_PASSWORD, setPasswordJson.toJSONString(),
                gdUser.getUsername());

        // Don't set the controlPanel value on the request, support users should not set the control panel password.
        SetPassword.Request setPasswordRequest = new SetPassword.Request();
        setPasswordRequest.hfsVmId = vm.hfsVmId;
        setPasswordRequest.usernames = Collections.singletonList(username);
        setPasswordRequest.encryptedPassword = cryptography.encrypt(password);

        Vps4SetSupportUserPassword.Request vps4SetSupportUserPasswordRequest = new Vps4SetSupportUserPassword.Request();
        vps4SetSupportUserPasswordRequest.actionId = actionId;
        vps4SetSupportUserPasswordRequest.vmId = vm.vmId;
        vps4SetSupportUserPasswordRequest.setPasswordRequest = setPasswordRequest;
        CommandState command = Commands.execute(commandService, actionService, "Vps4SetSupportUserPassword", vps4SetSupportUserPasswordRequest);

        logger.info("Changed support user password {} on vm {}", username, vmId);

        Action action = actionService.getAction(actionId);
        JSONObject message = new JSONObject();
        message.put("Username", username);
        message.put("Password", password);
        return new VmActionWithDetails(action, command, message.toString(), gdUser.isEmployee());
    }
}
