package com.godaddy.vps4.orchestration.sysadmin;

import com.godaddy.hfs.vm.VmService;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;

import gdg.hfs.orchestration.CommandRetryStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.ActionCommand;
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
        retryStrategy = CommandRetryStrategy.NEVER
)

public class Vps4RemoveSupportUser extends ActionCommand<Vps4RemoveSupportUser.Request, Void> {
    private final VmUserService vmUserService;
    private final VmService vmService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveSupportUser.class);

    @Inject
    public Vps4RemoveSupportUser(ActionService actionService, VmUserService vmUserService, VmService vmService) {
        super(actionService);
        this.vmUserService = vmUserService;
        this.vmService = vmService;
    }

    public static class Request extends Vps4ActionRequest {
        public long hfsVmId;
        public String username;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {

        if (vmService.getVm(request.hfsVmId).status.equalsIgnoreCase("STOPPED")) {
            rescheduleSupportUserRemoval(context, request);
            String errorMessage =
                    String.format(
                            "VM %s is in STOPPED status. Cannot remove support user %s from VM %s, scheduling retry.",
                            request.vmId, request.username, request.vmId);
            logger.warn(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        RemoveUser.Request removeUserRequest = new RemoveUser.Request();
        removeUserRequest.hfsVmId = request.hfsVmId;
        removeUserRequest.username = request.username;
        removeUserRequest.vmId = request.vmId;

        try {
            context.execute(RemoveUser.class, removeUserRequest);
            vmUserService.deleteUser(request.username, request.vmId);
        } catch (ActionNotCompletedException e) {
            String errorMessage =
                    String.format("Remove support user %s from VM %s failed, scheduling retry.", request.username,
                            request.vmId);
            logger.warn(errorMessage, e);
            rescheduleSupportUserRemoval(context, request);
            throw new RuntimeException(errorMessage, e);
        }

        return null;
    }

    private void rescheduleSupportUserRemoval(CommandContext context, Request request) {
        ScheduleSupportUserRemoval.Request removeSupportUserRequest = new ScheduleSupportUserRemoval.Request();
        removeSupportUserRequest.vmId = request.vmId;
        removeSupportUserRequest.username = request.username;
        context.execute(ScheduleSupportUserRemoval.class, removeSupportUserRequest);
    }
}
