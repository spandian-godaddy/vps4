package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.Vps4ActionRequest;
import com.godaddy.vps4.orchestration.panopta.ResumePanoptaMonitoring;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.scheduler.api.web.SchedulerWebService;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CommandMetadata(
        name = "Vps4MoveBack",
        requestType = Vps4MoveBack.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class Vps4MoveBack extends ActionCommand<Vps4MoveBack.Request, Void> {
    private CommandContext context;
    private final VirtualMachineService virtualMachineService;
    private final SchedulerWebService schedulerWebService;
    private final NetworkService networkService;
    private final PanoptaDataService panoptaDataService;
    private static final Logger logger = LoggerFactory.getLogger(Vps4MoveBack.class);

    @Inject
    public Vps4MoveBack(ActionService actionService,
                        VirtualMachineService virtualMachineService,
                        SchedulerWebService schedulerWebService,
                        NetworkService networkService,
                        PanoptaDataService panoptaDataService) {
        super(actionService);
        this.virtualMachineService = virtualMachineService;
        this.schedulerWebService = schedulerWebService;
        this.networkService = networkService;
        this.panoptaDataService = panoptaDataService;
    }

    public static class Request extends Vps4ActionRequest {
        public UUID vmId;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Request request) {
        this.context = context;

        VirtualMachine vm = virtualMachineService.getVirtualMachine(request.vmId);
        List<Long> addressIds = new ArrayList<>();
        List<IpAddress> additionalIps = networkService.getAllVmSecondaryAddresses(vm.hfsVmId);
        addressIds.add(vm.primaryIpAddress.addressId);
        addressIds.addAll(additionalIps.stream().map(ipAddress -> ipAddress.addressId).collect(Collectors.toList()));

        try {
            setVmCanceledAndValidUntil(request.vmId);
            setIpsValidUntil(addressIds);
            resumeAutomaticBackups(vm.backupJobId);
            markPanoptaServerActive(request.vmId);
            resumePanoptaMonitoring(request.vmId);
        } catch (Exception e) {
            String errorMessage = String.format("Move back failed for VM %s", request.vmId);
            logger.warn(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
        return null;
    }

    private void resumeAutomaticBackups(UUID backupJobId) {
        if (backupJobId != null) { // OH VMs will have a backupJobId of null, but we don't resume those snapshots ourselves.
            context.execute("ResumeAutomaticBackups", ctx -> {
                schedulerWebService.resumeJob("vps4", "backups", backupJobId);
                return null;
            }, Void.class);
        }
    }

    private void setVmCanceledAndValidUntil(UUID vmId) {
        context.execute("ClearVmCanceled", ctx -> {
            virtualMachineService.clearVmCanceled(vmId);
            return null;
        }, Void.class);

        context.execute("MarkVmAsActive", ctx -> {
            virtualMachineService.setVmActive(vmId);
            return null;
        }, Void.class);
    }

    private void markPanoptaServerActive(UUID vmId) {
        context.execute("MarkPanoptaServerActive", ctx -> {
            panoptaDataService.setPanoptaServerActive(vmId);
            return null;
        }, Void.class);
    }

    private void resumePanoptaMonitoring(UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        context.execute(ResumePanoptaMonitoring.class, vm);
    }

    private void setIpsValidUntil(List<Long> addressIds) {
        for (Long addressId : addressIds) {
            context.execute("MarkIpActive-" + addressId, ctx -> {
                networkService.activateIpAddress(addressId);
                return null;
            }, Void.class);
        }
    }
}
