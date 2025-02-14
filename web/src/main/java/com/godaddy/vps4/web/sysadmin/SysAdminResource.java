package com.godaddy.vps4.web.sysadmin;

import static com.godaddy.vps4.web.util.RequestValidation.validateHostname;
import static com.godaddy.vps4.web.util.RequestValidation.validateNoConflictingActions;
import static com.godaddy.vps4.web.util.RequestValidation.validateServerIsActive;

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
import com.godaddy.vps4.orchestration.sysadmin.Vps4SetCustomerPassword;
import com.godaddy.vps4.orchestration.sysadmin.Vps4ToggleAdmin;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.ActionType;
import com.godaddy.vps4.vm.Image.OperatingSystem;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.web.Vps4Api;
import com.godaddy.vps4.web.Vps4Exception;
import com.godaddy.vps4.web.security.GDUser;
import com.godaddy.vps4.web.util.Commands;
import com.godaddy.vps4.vm.VmAction;
import com.godaddy.vps4.web.vm.VmResource;

import gdg.hfs.orchestration.CommandService;
import io.swagger.annotations.Api;

@Vps4Api
@Api(tags = { "vms" })

@Path("/api/vms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SysAdminResource {

    private static final Logger logger = LoggerFactory.getLogger(SysAdminResource.class);

    final VmResource vmResource;
    final VmUserService vmUserService;
    final ActionService actionService;
    final CommandService commandService;
    final VirtualMachineService virtualMachineService;
    final GDUser user;
    private final Cryptography cryptography;

    @Inject
    public SysAdminResource(VmResource vmResource,
                            VmUserService userService,
                            ActionService actionService,
                            CommandService commandService,
                            GDUser user,
                            VirtualMachineService virtualMachineService,
                            Cryptography cryptography) {
        this.vmResource = vmResource;
        this.vmUserService = userService;
        this.actionService = actionService;
        this.commandService = commandService;
        this.user = user;
        this.virtualMachineService = virtualMachineService;
        this.cryptography = cryptography;
    }

    public static class UpdatePasswordRequest{
        public String username;
        public String password;
    }

    @POST
    @Path("/{vmId}/setPassword")
    public VmAction setPassword(@PathParam("vmId") UUID vmId, UpdatePasswordRequest updatePasswordRequest) {
        VirtualMachine vm = vmResource.getVm(vmId);

        validateServerIsActive(vmResource.getVmFromVmVertical(vm.hfsVmId));
        validateNoConflictingActions(vmId, actionService, ActionType.SET_PASSWORD, ActionType.RESTORE_VM);

        if (!vmUserService.userExists(updatePasswordRequest.username, vm.vmId) && !isMigratedRootRequest(updatePasswordRequest.username, vm.vmId)) {
            String message = String.format("Cannot find user %s for vm %s",  updatePasswordRequest.username, vmId);
            throw new Vps4Exception("VM_USER_NOT_FOUND", message);
        }

        JSONObject pwRequest = new JSONObject();
        pwRequest.put("username", updatePasswordRequest.username);

        long actionId = actionService.createAction(vm.vmId, ActionType.SET_PASSWORD,
                pwRequest.toJSONString(), user.getUsername());

        List<String> usernames = new ArrayList<String>();
        usernames.add(updatePasswordRequest.username);
        if(vm.image.operatingSystem == OperatingSystem.LINUX)
            usernames.add("root");

        SetPassword.Request request = new SetPassword.Request();
        request.usernames = usernames;
        request.encryptedPassword = cryptography.encrypt(updatePasswordRequest.password);
        request.hfsVmId = vm.hfsVmId;
        request.controlPanel = vm.image.getImageControlPanel();

        Vps4SetCustomerPassword.Request vps4Request = new Vps4SetCustomerPassword.Request();
        vps4Request.actionId = actionId;
        vps4Request.vmId = vm.vmId;
        vps4Request.setPasswordRequest = request;
        vps4Request.controlPanel = vm.image.controlPanel;

        Commands.execute(commandService, actionService, "Vps4SetPassword", vps4Request);

        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }
    private boolean isMigratedRootRequest(String username, UUID vmId) {
        return username.equals("root") && virtualMachineService.isLinux(vmId) && virtualMachineService.getImportedVm(vmId) != null;
    }

    public static class SetHostnameRequest{
        public String hostname;
    }

    @POST
    @Path("/{vmId}/setHostname")
    public VmAction setHostname(@PathParam("vmId") UUID vmId, SetHostnameRequest setHostnameRequest) {
        VirtualMachine vm = vmResource.getVm(vmId);

        validateServerIsActive(vmResource.getVmFromVmVertical(vm.hfsVmId));
        validateNoConflictingActions(vmId, actionService, ActionType.SET_HOSTNAME);
        validateHostname(setHostnameRequest.hostname, vm.image);

        JSONObject hostnameJsonRequest = new JSONObject();
        hostnameJsonRequest.put("hostname", setHostnameRequest.hostname);

        long actionId = actionService.createAction(vmId, ActionType.SET_HOSTNAME,
                hostnameJsonRequest.toJSONString(), user.getUsername());

        SetHostname.Request hfsRequest = new SetHostname.Request(vm.hfsVmId, setHostnameRequest.hostname, vm.image.getImageControlPanel());

        Vps4SetHostname.Request vps4Request = new Vps4SetHostname.Request();
        vps4Request.setHostnameRequest = hfsRequest;
        vps4Request.vmId = vmId;
        vps4Request.oldHostname = vm.hostname;
        vps4Request.actionId = actionId;

        Commands.execute(commandService, actionService, "Vps4SetHostname", vps4Request);

        return new VmAction(actionService.getAction(actionId), user.isEmployee());

    }

    public static class SetAdminRequest {
        public String username;
    }

    @POST
    @Path("/{vmId}/enableAdmin")
    public VmAction enableUserAdmin(@PathParam("vmId") UUID vmId, SetAdminRequest setAdminRequest) {
        return setUserAdmin(setAdminRequest.username, vmId, true);
    }

    @POST
    @Path("/{vmId}/disableAdmin")
    public VmAction disableUserAdmin(@PathParam("vmId") UUID vmId, SetAdminRequest setAdminRequest) {
        return setUserAdmin(setAdminRequest.username, vmId, false);
    }

    private VmAction setUserAdmin(String username, UUID vmId, boolean shouldEnable) {
        logger.info("Username: {} VmId: {} Enable admin access? {}", username, vmId, shouldEnable);
        VirtualMachine vm = vmResource.getVm(vmId);

        ActionType actionType = shouldEnable ? ActionType.ENABLE_ADMIN_ACCESS : ActionType.DISABLE_ADMIN_ACCESS;
        validateServerIsActive(vmResource.getVmFromVmVertical(vm.hfsVmId));
        validateNoConflictingActions(vmId, actionService, actionType, ActionType.RESTORE_VM);

        if (!vmUserService.userExists(username, vm.vmId)) {
            throw new Vps4Exception("VM_USER_NOT_FOUND", "User not found on virtual machine.");
        }

        JSONObject adminRequest = new JSONObject();
        adminRequest.put("username", username);
        adminRequest.put("enabled", shouldEnable);

        long actionId = actionService.createAction(vm.vmId, actionType, adminRequest.toJSONString(), user.getUsername());

        Vps4ToggleAdmin.Request vps4Request = new Vps4ToggleAdmin.Request();
        vps4Request.actionId = actionId;
        vps4Request.enabled = shouldEnable;
        vps4Request.hfsVmId = vm.hfsVmId;
        vps4Request.vmId = vm.vmId;
        vps4Request.username = username;

        Commands.execute(commandService, actionService, "Vps4ToggleAdmin", vps4Request);

        return new VmAction(actionService.getAction(actionId), user.isEmployee());
    }
}
