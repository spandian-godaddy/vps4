package com.godaddy.vps4.orchestration.vm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.cpanel.WaitForCpanelAction;
import com.godaddy.vps4.orchestration.hfs.plesk.WaitForPleskAction;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.orchestration.scheduler.DeleteAutomaticBackupSchedule;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.plesk.PleskAction;
import gdg.hfs.vhfs.plesk.PleskService;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(name = "Vps4DestroyVm", requestType = Vps4DestroyVm.Request.class, responseType = Vps4DestroyVm.Response.class)
public class Vps4DestroyVm extends ActionCommand<Vps4DestroyVm.Request, Vps4DestroyVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyVm.class);
    private final NetworkService networkService;
    private final VirtualMachineService virtualMachineService;
    private final VmService vmService;
    private final CPanelService cpanelService;
    private final PleskService pleskService;
    private final NodePingService monitoringService;
    CommandContext context;

    @Inject
    public Vps4DestroyVm(ActionService actionService, NetworkService networkService,
            VirtualMachineService virtualMachineService, VmService vmService, CPanelService cpanelService,
            NodePingService monitoringService, PleskService pleskService) {
        super(actionService);
        this.networkService = networkService;
        this.virtualMachineService = virtualMachineService;
        this.vmService = vmService;
        this.cpanelService = cpanelService;
        this.monitoringService = monitoringService;
        this.pleskService = pleskService;
    }

    @Override
    public Response executeWithAction(CommandContext context, Vps4DestroyVm.Request request) {
        this.context = context;

        logger.info("Destroying VM {}", request.virtualMachine.vmId);
        VirtualMachine vm = request.virtualMachine;

        unlicenseControlPanel(vm);
        releaseIps(context, request, vm);
        deleteAutomaticBackupSchedule(vm);
        deleteAllScheduledJobsForVm(context, vm);
        VmAction hfsAction = deleteVmInHfs(context, vm);

        logger.info("Completed destroying VM {}", vm.vmId);

        Vps4DestroyVm.Response response = new Vps4DestroyVm.Response();
        response.vmId = vm.vmId;
        response.hfsAction = hfsAction;
        return response;
    }

    private VmAction deleteVmInHfs(CommandContext context, VirtualMachine vm) {
        if (vm.hfsVmId != 0) {
            VmAction hfsAction = context.execute("DestroyVmHfs", ctx -> vmService.destroyVm(vm.hfsVmId),
                    VmAction.class);

            hfsAction = context.execute(WaitForVmAction.class, hfsAction);
            return hfsAction;
        } else {
            return null;
        }
    }

    private void deleteAllScheduledJobsForVm(CommandContext context, VirtualMachine vm) {
        context.execute("DestroyAllScheduledJobsForVm", Vps4DeleteAllScheduledJobsForVm.class, vm.vmId);
    }

    private void releaseIps(CommandContext context, Vps4DestroyVm.Request request, VirtualMachine vm) {
        List<IpAddress> activeAddresses = networkService.getVmIpAddresses(vm.vmId).stream()
                .filter(address -> address.validUntil.isAfter(Instant.now())).collect(Collectors.toList());

        for (IpAddress address : activeAddresses) {
            if (address.pingCheckId != null) {
                monitoringService.deleteCheck(request.pingCheckAccountId, address.pingCheckId);
            }

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

    public static class Request extends VmActionRequest {
        public long pingCheckAccountId;
    }

    public static class Response {
        public UUID vmId;
        public VmAction hfsAction;
    }

}
