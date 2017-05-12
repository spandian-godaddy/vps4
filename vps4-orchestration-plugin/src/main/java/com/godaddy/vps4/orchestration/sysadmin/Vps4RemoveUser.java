package com.godaddy.vps4.orchestration.sysadmin;

import java.util.UUID;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.RemoveUser;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
        name = "Vps4RemoveUser",
        requestType = Vps4RemoveUser.Request.class,
        responseType = Void.class
)
public class Vps4RemoveUser extends ActionCommand<Vps4RemoveUser.Request, Void> {

    private final VmUserService vmUserService;

    @Inject
    public Vps4RemoveUser(ActionService actionService, VmUserService vmUserService) {
        super(actionService);
        this.vmUserService = vmUserService;
    }

    public static class Request implements ActionRequest {
        public long actionId;
        public long hfsVmId;
        public UUID vmId;
        public String username;

        @Override
        public long getActionId() {
            return actionId;
        }
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request req) throws Exception {

        RemoveUser.Request removeUserRequest = new RemoveUser.Request();
        removeUserRequest.hfsVmId = req.hfsVmId;
        removeUserRequest.username = req.username;
        removeUserRequest.vmId = req.vmId;
        context.execute(RemoveUser.class, removeUserRequest);

        vmUserService.deleteUser(req.username, req.vmId);

        return null;
    }

}
