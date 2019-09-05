package com.godaddy.vps4.orchestration.vm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.hfs.vm.VmAction;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.DestroyVm;
import com.godaddy.vps4.orchestration.monitoring.Vps4RemoveMonitoring;
import com.godaddy.vps4.orchestration.scheduler.DeleteAutomaticBackupSchedule;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.plesk.PleskService;

@CommandMetadata(
        name="Vps4DestroyVm",
        requestType=VmActionRequest.class,
        responseType=Vps4DestroyVm.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4DestroyVm extends ActionCommand<VmActionRequest, Vps4DestroyVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyVm.class);
    private final NetworkService networkService;
    CommandContext context;

    @Inject
    public Vps4DestroyVm(ActionService actionService,
            NetworkService networkService,
            VirtualMachineService virtualMachineService,
            CPanelService cpanelService,
            PleskService pleskService) {
        super(actionService);
        this.networkService = networkService;
    }

    @Override
    public Response executeWithAction(CommandContext context, VmActionRequest request) {
        this.context = context;

        logger.info("Destroying VM {}", request.virtualMachine.vmId);
        VirtualMachine vm = request.virtualMachine;

        unlicenseControlPanel(vm);
        removeMonitoring(vm);
        releaseIps(context, request, vm);
        deleteAutomaticBackupSchedule(vm);
        deleteAllScheduledJobsForVm(context, vm);
        deleteSupportUsersInDatabase(context, vm);
        VmAction hfsAction = deleteVmInHfs(context, vm, request);

        logger.info("Completed destroying VM {}", vm.vmId);

        Vps4DestroyVm.Response response = new Vps4DestroyVm.Response();
        response.vmId = vm.vmId;
        response.hfsAction = hfsAction;
        return response;
    }

    private VmAction deleteVmInHfs(CommandContext context, VirtualMachine vm, VmActionRequest request) {
        DestroyVm.Request destroyVmRequest = new DestroyVm.Request();
        destroyVmRequest.hfsVmId = vm.hfsVmId;
        destroyVmRequest.actionId = request.actionId;
        return context.execute("DestroyVmHfs", DestroyVm.class, destroyVmRequest);
    }

    private void deleteSupportUsersInDatabase(CommandContext context, VirtualMachine vm) {
        context.execute("DestroyAllSupportUsersForVm", Vps4RemoveSupportUsersFromDatabase.class, vm.vmId);
    }

    private void deleteAllScheduledJobsForVm(CommandContext context, VirtualMachine vm) {
        context.execute("DestroyAllScheduledJobsForVm", Vps4DeleteAllScheduledJobsForVm.class, vm.vmId);
    }

    private void removeMonitoring(VirtualMachine vm) {
        context.execute(Vps4RemoveMonitoring.class, vm.vmId);
    }

    private void releaseIps(CommandContext context, VmActionRequest request, VirtualMachine vm) {
        List<IpAddress> activeAddresses = networkService.getVmIpAddresses(vm.vmId).stream()
                .filter(address -> address.validUntil.isAfter(Instant.now())).collect(Collectors.toList());

        for (IpAddress address : activeAddresses) {
            context.execute("DeleteIpAddress-" + address.ipAddressId, Vps4DestroyIpAddress.class,
                    new Vps4DestroyIpAddress.Request(address, vm, true));
            context.execute("Destroy-" + address.ipAddressId, ctx -> {
                networkService.destroyIpAddress(address.ipAddressId);
                return null;
            }, Void.class);
        }
    }

    private void deleteAutomaticBackupSchedule(VirtualMachine vm) {
        if (hasAutomaticBackupJobScheduled(vm)) {
            try {
                context.execute(DeleteAutomaticBackupSchedule.class, vm.backupJobId);
            } catch (RuntimeException e) {
                // squelch this for now. dont fail a vm deletion just because we couldn't delete an auto backup schedule
                // TODO: should this behaviour be changed?
                logger.error("Automatic backup job schedule deletion failed");
            }
        }
    }

    private boolean hasAutomaticBackupJobScheduled(VirtualMachine vm) {
        return vm.backupJobId != null;
    }

    private void unlicenseControlPanel(VirtualMachine vm) {
            context.execute(UnlicenseControlPanel.class, vm);
    }

    public static class Response {
        public UUID vmId;
        public VmAction hfsAction;
    }

}
