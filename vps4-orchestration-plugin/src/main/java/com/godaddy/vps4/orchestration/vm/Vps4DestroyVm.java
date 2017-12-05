package com.godaddy.vps4.orchestration.vm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.godaddy.vps4.orchestration.scheduler.DeleteAutomaticBackupSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.cpanel.WaitForCpanelAction;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.vhfs.cpanel.CPanelAction;
import gdg.hfs.vhfs.cpanel.CPanelService;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.vm.Vm;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;

@CommandMetadata(
        name="Vps4DestroyVm",
        requestType=Vps4DestroyVm.Request.class,
        responseType=Vps4DestroyVm.Response.class
    )
public class Vps4DestroyVm extends ActionCommand<Vps4DestroyVm.Request, Vps4DestroyVm.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyVm.class);

    private final ActionService actionService;

    private final NetworkService networkService;

    private final VirtualMachineService virtualMachineService;

    private final CreditService creditService;

    private final VmService vmService;

    private final CPanelService cpanelService;

    private final NodePingService monitoringService;

    CommandContext context;

    @Inject
    public Vps4DestroyVm(ActionService actionService,
            NetworkService networkService,
            VirtualMachineService virtualMachineService,
            CreditService creditService,
            VmService vmService,
            CPanelService cpanelService,
            NodePingService monitoringService) {
        super(actionService);
        this.actionService = actionService;
        this.networkService = networkService;
        this.virtualMachineService = virtualMachineService;
        this.creditService = creditService;
        this.vmService = vmService;
        this.cpanelService = cpanelService;
        this.monitoringService = monitoringService;
    }


    @Override
    public Response executeWithAction(CommandContext context, Vps4DestroyVm.Request request) {
        this.context = context;

        final long hfsVmId = request.hfsVmId;
        VirtualMachine vm = this.virtualMachineService.getVirtualMachine(hfsVmId);
        logger.info("Destroying VM {} with hfsVmId {}", vm.vmId, hfsVmId);

        unlicenseCpanel(hfsVmId, vm.vmId);

        List<IpAddress> addresses = networkService.getVmIpAddresses(vm.vmId);
        if (addresses != null){
            // filter out all previously removed IPs
            addresses = addresses.stream().filter(address -> address.validUntil.isAfter(Instant.now())).collect(Collectors.toList());
        }

        for (IpAddress address : addresses) {
            if(address.pingCheckId != null ){
                monitoringService.deleteCheck(request.pingCheckAccountId, address.pingCheckId);
            }

            context.execute("DeleteIpAddress-" + address.ipAddressId, Vps4DestroyIpAddress.class,
                    new Vps4DestroyIpAddress.Request(address, vm, true));
            context.execute("Destroy-"+address.ipAddressId, ctx -> {networkService.destroyIpAddress(address.ipAddressId);
                                                                    return null;}, Void.class);
        }

        if (hasAutomaticBackupJobScheduled(vm)) {
            deleteAutomaticBackupSchedule(vm.backupJobId);
        }

        context.execute("DestroyAllScheduledJobsForVm", Vps4DeleteAllScheduledJobsForVm.class, vm.vmId);

        VmAction hfsAction = context.execute("DestroyVmHfs", ctx -> vmService.destroyVm(hfsVmId), VmAction.class);

        hfsAction = context.execute(WaitForVmAction.class, hfsAction);

        logger.info("Completed destroying VM {} with hfsVmId {}", vm.vmId, hfsVmId);

        Vps4DestroyVm.Response response = new Vps4DestroyVm.Response();
        response.vmId = hfsVmId;
        response.hfsAction = hfsAction;
        return response;
    }

    private boolean hasAutomaticBackupJobScheduled(VirtualMachine vm) {
        return vm.backupJobId != null;
    }

    private void deleteAutomaticBackupSchedule(UUID backupJobId) {
        try {
            context.execute(DeleteAutomaticBackupSchedule.class, backupJobId);
        }
        catch (RuntimeException e) {
            // squelch this for now. dont fail a vm deletion just because we couldn't delete an auto backup schedule
            // TODO: should this behaviour be changed?
            logger.error("Automatic backup job schedule deletion failed");
        }
    }

    private void unlicenseCpanel(final long hfsVmId, UUID vmId) {
        if(this.virtualMachineService.virtualMachineHasCpanel(vmId)){
            Vm hfsVm = vmService.getVm(hfsVmId);
            CPanelAction action = context.execute("Unlicense-Cpanel", ctx -> {
                return cpanelService.licenseRelease(hfsVmId);
            }, CPanelAction.class);
            context.execute(WaitForCpanelAction.class, action);
        }
    }

    public static class Request extends VmActionRequest {
        public long pingCheckAccountId;
    }

    public static class Response {
        public long vmId;
        public VmAction hfsAction;
    }

}
