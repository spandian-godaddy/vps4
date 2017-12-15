package com.godaddy.vps4.orchestration.sysadmin;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.ActionRequest;
import com.godaddy.vps4.orchestration.NoRetryException;
import com.godaddy.vps4.orchestration.hfs.ActionNotCompletedException;
import com.godaddy.vps4.orchestration.hfs.sysadmin.RemoveUser;
import com.godaddy.vps4.orchestration.scheduler.ScheduleSupportUserRemoval;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmUserService;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;

@CommandMetadata(
        name = "Vps4RemoveSupportUser",
        requestType = Vps4RemoveSupportUser.Request.class,
        responseType = Void.class
)
public class Vps4RemoveSupportUser extends ActionCommand<Vps4RemoveSupportUser.Request, Void> {

    private final VmUserService vmUserService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveSupportUser.class);

    @Inject
    public Vps4RemoveSupportUser(ActionService actionService, VmUserService vmUserService) {
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

        @Override
        public void setActionId(long actionId) {
            this.actionId = actionId;
        }
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) throws Exception {

        RemoveUser.Request removeUserRequest = new RemoveUser.Request();
        removeUserRequest.hfsVmId = request.hfsVmId;
        removeUserRequest.username = request.username;
        removeUserRequest.vmId = request.vmId;

        try {
            context.execute(RemoveUser.class, removeUserRequest);
            vmUserService.deleteUser(request.username, request.vmId);
        } catch (ActionNotCompletedException e) {
            String errorMessage = String.format("Remove support user from VM %s failed, scheduling retry.", request.vmId);
            logger.warn(errorMessage, e);
            ScheduleSupportUserRemoval.Request removeSupportUserRequest = new ScheduleSupportUserRemoval.Request();
            removeSupportUserRequest.vmId = request.vmId;
            context.execute(ScheduleSupportUserRemoval.class, removeSupportUserRequest);
            throw new NoRetryException(errorMessage, e);
        }


        return null;
    }

}
