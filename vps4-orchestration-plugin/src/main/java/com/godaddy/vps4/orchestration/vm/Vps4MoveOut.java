package com.godaddy.vps4.orchestration.vm;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.hfs.sysadmin.UninstallPanoptaAgent;
import com.godaddy.vps4.orchestration.monitoring.RemovePanoptaMonitoring;
import com.godaddy.vps4.orchestration.sysadmin.Vps4RemoveSupportUser;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.godaddy.vps4.vm.VmUserService;
import com.godaddy.vps4.vm.VmUserType;
import com.google.inject.Inject;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

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
    private final NetworkService networkService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4MoveOut.class);

    @Inject
    public Vps4MoveOut(ActionService actionService,
                       VirtualMachineService virtualMachineService,
                       VmUserService vmUserService,
                       SchedulerWebService schedulerWebService,
                       NetworkService networkService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
        this.vmUserService = vmUserService;
        this.schedulerWebService = schedulerWebService;
        this.networkService = networkService;
    }

    public static class Request extends Vps4ActionRequest {
        public UUID vmId;
        public UUID backupJobId;
        public long hfsVmId;
        public List<Long> addressIds;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;

        try {
            removeSupportUsers(request.vmId, request.hfsVmId);
            pauseAutomaticBackups(request.backupJobId);
            setVmCanceledAndValidUntil(request.vmId);
            setIpsValidUntil(request.addressIds);
            removeMonitoring(request.vmId, request.hfsVmId);
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
            Vps4RemoveSupportUser.Request r = new Vps4RemoveSupportUser.Request();
            r.hfsVmId = hfsVmId;
            r.username = username;

            context.execute("AttemptRemoveUser-" + r.username, ctx -> {
                try {
                    context.execute("RemoveUser-" + r.username, Vps4RemoveSupportUser.class, r);
                } catch (Exception e) {
                    logger.error("Exception while removing support user {} for VM {}",
                                 r.username, r.vmId, e);
                }
                return null;
            }, Void.class);
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

    private void setVmCanceledAndValidUntil(UUID vmId) {
        context.execute("MarkVmAsZombie", ctx -> {
            virtualMachineService.setVmCanceled(vmId);
            return null;
        }, Void.class);

        context.execute("MarkVmAsRemoved", ctx -> {
            virtualMachineService.setVmRemoved(vmId);
            return null;
        }, Void.class);
    }

    private void setIpsValidUntil(List<Long> addressIds) {
        for (Long addressId : addressIds) {
            context.execute("MarkIpDeleted-" + addressId, ctx -> {
                networkService.destroyIpAddress(addressId);
                return null;
            }, Void.class);
        }
    }

    private void removeMonitoring(UUID vmId, long hfsVmId) {
        try {
            // uninstalling the agent greatly improves the chances that a reinstall will work
            context.execute(UninstallPanoptaAgent.class, hfsVmId);
        } catch (Exception ignored) {}
        RemovePanoptaMonitoring.Request request = new RemovePanoptaMonitoring.Request();
        request.vmId = vmId;
        request.orionGuid = virtualMachineService.getOrionGuidByVmId(vmId);
        context.execute(RemovePanoptaMonitoring.class, request);
    }
}
