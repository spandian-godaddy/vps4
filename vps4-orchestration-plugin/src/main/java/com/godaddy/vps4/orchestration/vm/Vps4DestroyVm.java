package com.godaddy.vps4.orchestration.vm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
import com.godaddy.vps4.orchestration.scheduler.ScheduleDestroyVm;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

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
    VirtualMachine vm;

    @Inject
    public Vps4DestroyVm(ActionService actionService, NetworkService networkService) {
        super(actionService);
        this.networkService = networkService;
    }

    @Override
    public Response executeWithAction(CommandContext context, VmActionRequest request) {
        this.context = context;
        this.vm = request.virtualMachine;

        logger.info("Destroying server {}", vm.vmId);

        try {
            unlicenseControlPanel();
            removeMonitoring();
            removeIp();
            deleteAutomaticBackupSchedule();
            deleteAllScheduledJobsForVm();
            deleteSupportUsersInDatabase();
            VmAction hfsAction = deleteVmInHfs(request);
            return buildResponse(hfsAction);
        } catch (Exception e) {
            rescheduleDestroy();
            throw e;
        }
    }

    private Response buildResponse(VmAction hfsAction) {
        logger.info("Completed destroying VM {}", vm.vmId);

        Vps4DestroyVm.Response response = new Vps4DestroyVm.Response();
        response.vmId = vm.vmId;
        response.hfsAction = hfsAction;
        return response;
    }

    private void rescheduleDestroy() {
        context.execute(ScheduleDestroyVm.class, vm.vmId);
    }

    private VmAction deleteVmInHfs(VmActionRequest request) {
        DestroyVm.Request destroyVmRequest = new DestroyVm.Request();
        destroyVmRequest.hfsVmId = vm.hfsVmId;
        destroyVmRequest.actionId = request.actionId;
        return context.execute("DestroyVmHfs", DestroyVm.class, destroyVmRequest);
    }

    private void deleteSupportUsersInDatabase() {
        context.execute(Vps4RemoveSupportUsersFromDatabase.class, vm.vmId);
    }

    private void deleteAllScheduledJobsForVm() {
        context.execute(Vps4DeleteAllScheduledJobsForVm.class, vm.vmId);
    }

    private void removeMonitoring() {
        context.execute(Vps4RemoveMonitoring.class, vm.vmId);
    }

    private void removeIp() {
        IpAddress address = networkService.getVmPrimaryAddress(vm.vmId);
        if (isAddressValid(address)) {
            removeIpFromServer(address);
            updateIpValidUntil(address);
        }
        //clean up additional ips if they exist in OH and Ded
        getAndRemoveAdditionalIps();
    }
    private void getAndRemoveAdditionalIps() {
        List<IpAddress> additionalIps;
        additionalIps = networkService.getVmSecondaryAddress(vm.hfsVmId);
        if (additionalIps != null) {
            for (IpAddress ip : additionalIps) {
                context.execute("RemoveIp-" + ip.addressId, Vps4RemoveIp.class, ip);
                context.execute("MarkIpDeleted-" + ip.addressId, ctx -> {
                    networkService.destroyIpAddress(ip.addressId);
                    return null;
                }, Void.class);
            }
        }
    }

    private boolean isAddressValid(IpAddress address) {
        return address != null && address.validUntil.isAfter(Instant.now());
    }


    private void updateIpValidUntil(IpAddress address) {
        context.execute("MarkIpDeleted-" + address.addressId, ctx -> {
                networkService.destroyIpAddress(address.addressId);
                return null;
            }, Void.class);
    }

    protected void removeIpFromServer(IpAddress address) {
        context.execute("RemoveIp-" + address.addressId, Vps4RemoveIp.class, address);
    }

    private void deleteAutomaticBackupSchedule() {
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

    private void unlicenseControlPanel() {
        context.execute(UnlicenseControlPanel.class, vm);
    }

    public static class Response {
        public UUID vmId;
        public VmAction hfsAction;
    }

}
