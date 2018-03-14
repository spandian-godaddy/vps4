package com.godaddy.vps4.orchestration.sysadmin;

import java.util.UUID;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.orchestration.scheduler.ScheduleSupportUserRemoval;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
        name = "Vps4AddSupportUser",
        requestType = Vps4AddSupportUser.Request.class,
        responseType = Void.class
)
public class Vps4AddSupportUser extends ActionCommand<Vps4AddSupportUser.Request, Void> {

    private final VmUserService vmUserService;

    @Inject
    public Vps4AddSupportUser(ActionService actionService, VmUserService vmUserService) {
        super(actionService);
        this.vmUserService = vmUserService;
    }

    public static class Request implements ActionRequest {
        public long actionId;
        public long hfsVmId;
        public UUID vmId;
        public String username;
        public byte[] encryptedPassword;

        @Override
        public long getActionId() {
            return actionId;
        }

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request req) throws Exception {

        AddUser.Request addUserRequest = new AddUser.Request();
        addUserRequest.hfsVmId = req.hfsVmId;
        addUserRequest.username = req.username;
        addUserRequest.encryptedPassword = req.encryptedPassword;
        context.execute(AddUser.class, addUserRequest);

        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.vmId = req.hfsVmId;
        toggleAdminRequest.username = req.username;
        toggleAdminRequest.enabled = true;
        context.execute(ToggleAdmin.class, toggleAdminRequest);

        context.execute("AddSupportUserToDatabase", ctx -> {
            vmUserService.createUser(req.username, req.vmId, true, VmUserType.SUPPORT);
            return null;
        }, Void.class);

        ScheduleSupportUserRemoval.Request removeSupportUserRequest = new ScheduleSupportUserRemoval.Request();
        removeSupportUserRequest.vmId = req.vmId;
        context.execute(ScheduleSupportUserRemoval.class, removeSupportUserRequest);

        return null;
    }

}
