package com.godaddy.vps4.orchestration.sysadmin;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.scheduler.ScheduleSupportUserRemoval;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4AddSupportUser",
        requestType = Vps4AddSupportUser.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4AddSupportUser extends ActionCommand<Vps4AddSupportUser.Request, Void> {

    private final VmUserService vmUserService;

    @Inject
    public Vps4AddSupportUser(ActionService actionService, VmUserService vmUserService) {
        super(actionService);
        this.vmUserService = vmUserService;
    }

    public static class Request extends Vps4ActionRequest {
        public long hfsVmId;
        public String username;
        public byte[] encryptedPassword;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request req) {
        // adds the user through HFS
        AddUser.Request addUserRequest = new AddUser.Request();
        addUserRequest.hfsVmId = req.hfsVmId;
        addUserRequest.username = req.username;
        addUserRequest.encryptedPassword = req.encryptedPassword;
        context.execute(AddUser.class, addUserRequest);

        // makes the user an admin
        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.vmId = req.hfsVmId;
        toggleAdminRequest.username = req.username;
        toggleAdminRequest.enabled = true;
        context.execute(ToggleAdmin.class, toggleAdminRequest);

        // add user to VPS4 database
        context.execute("AddSupportUserToDatabase", ctx -> {
            vmUserService.createUser(req.username, req.vmId, true, VmUserType.SUPPORT);
            return null;
        }, Void.class);

        // schedule removal of support user
        ScheduleSupportUserRemoval.Request removeSupportUserRequest = new ScheduleSupportUserRemoval.Request();
        removeSupportUserRequest.vmId = req.vmId;
        removeSupportUserRequest.username = req.username;
        context.execute(ScheduleSupportUserRemoval.class, removeSupportUserRequest);

        return null;
    }

}
