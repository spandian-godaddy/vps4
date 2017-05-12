package com.godaddy.vps4.orchestration.sysadmin;

import java.util.UUID;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.AddUser;
import com.godaddy.vps4.orchestration.hfs.sysadmin.ToggleAdmin;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
        name = "Vps4AddAdminUser",
        requestType = Vps4AddAdminUser.Request.class,
        responseType = Void.class
)
public class Vps4AddAdminUser extends ActionCommand<Vps4AddAdminUser.Request, Void> {

    private final VmUserService vmUserService;

    @Inject
    public Vps4AddAdminUser(ActionService actionService, VmUserService vmUserService) {
        super(actionService);
        this.vmUserService = vmUserService;
    }

    public static class Request implements ActionRequest {
        public long actionId;
        public long hfsVmId;
        public UUID vmId;
        public String username;
        public String password;

        @Override
        public long getActionId() {
            return actionId;
        }
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request req) throws Exception {

        AddUser.Request addUserRequest = new AddUser.Request();
        addUserRequest.hfsVmId = req.hfsVmId;
        addUserRequest.username = req.username;
        addUserRequest.password = req.password;
        context.execute(AddUser.class, addUserRequest);

        ToggleAdmin.Request toggleAdminRequest = new ToggleAdmin.Request();
        toggleAdminRequest.vmId = req.hfsVmId;
        toggleAdminRequest.username = req.username;
        toggleAdminRequest.enabled = true;
        context.execute(ToggleAdmin.class, toggleAdminRequest);

        vmUserService.createUser(req.username, req.vmId, true);
        // TODO: Add scheduled task to remove this user when scheduler is available.

        return null;
    }

}
