package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.orchestration.panopta.PausePanoptaMonitoring;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.Inject;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CommandMetadata(
        name = "Vps4MoveOut",
        requestType = Vps4MoveOut.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class Vps4MoveOut extends ActionCommand<Vps4MoveOut.Request, Void> {
    private CommandContext context;
    private final VirtualMachineService virtualMachineService;
    private final VmUserService vmUserService;
    private final SchedulerWebService schedulerWebService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4MoveOut.class);

    @Inject
    public Vps4MoveOut(ActionService actionService,
                       VirtualMachineService virtualMachineService,
                       VmUserService vmUserService,
                       SchedulerWebService schedulerWebService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.schedulerWebService = schedulerWebService;
    }

    public static class Request extends Vps4ActionRequest {
        public UUID vmId;
        public UUID backupJobId;
        public long hfsVmId;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;

        try {
            removeSupportUsers(request.vmId, request.hfsVmId);
            pauseAutomaticBackups(request.backupJobId);
            setCanceledAndValidUntil(request.vmId);
            pausePanoptaMonitoring(request.vmId);
        } catch (Exception e) {
            String errorMessage = String.format("Move out failed for VM %s", request.vmId);
            logger.warn(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
        return null;
    }

    private void removeSupportUsers(UUID vmId, long hfsVmId) {
        List<String> supportUsernames = vmUserService.listUsers(vmId, VmUserType.SUPPORT).stream()
                .map(u -> u.username).collect(Collectors.toList());

        for (String username : supportUsernames) {
            Vps4RemoveSupportUser.Request removeSupportUserRequest = new Vps4RemoveSupportUser.Request();
            removeSupportUserRequest.hfsVmId = hfsVmId;
            removeSupportUserRequest.username = username;

            context.execute("RemoveUser-" + removeSupportUserRequest.username,
                    Vps4RemoveSupportUser.class, removeSupportUserRequest);
        }
    }

    private void pauseAutomaticBackups(UUID backupJobId) {
        if (backupJobId != null) { // OH VMs will have a backupJobId of null, but we don't pause those snapshots ourselves.
            context.execute("PauseAutomaticBackups", ctx -> {
                schedulerWebService.pauseJob("vps4", "backups", backupJobId);
                return null;
            }, Void.class);
        }
    }

    private void setCanceledAndValidUntil(UUID vmId) {
        context.execute("MarkVmAsZombie", ctx -> {
            virtualMachineService.setVmZombie(vmId);
            return null;
        }, Void.class);

        context.execute("MarkVmAsRemoved", ctx -> {
            virtualMachineService.setVmRemoved(vmId);
            return null;
        }, Void.class);
    }

    private void pausePanoptaMonitoring(UUID vmId) {
        context.execute(PausePanoptaMonitoring.class, vmId);
    }
}
