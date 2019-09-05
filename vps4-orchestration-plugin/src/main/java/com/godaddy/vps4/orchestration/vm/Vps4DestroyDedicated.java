package com.godaddy.vps4.orchestration.vm;

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
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;

import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4DestroyDedicated",
        requestType = VmActionRequest.class,
        responseType = Vps4DestroyDedicated.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4DestroyDedicated extends ActionCommand<VmActionRequest, Vps4DestroyDedicated.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyDedicated.class);
    private final NetworkService networkService;
    CommandContext context;

    @Inject
    public Vps4DestroyDedicated(ActionService actionService, NetworkService networkService) {
        super(actionService);
        this.networkService = networkService;
    }

    @Override
    public Response executeWithAction(CommandContext context, VmActionRequest request) {
        this.context = context;

        logger.info("Destroying Dedicated Server {}", request.virtualMachine.vmId);
        VirtualMachine vm = request.virtualMachine;

        unlicenseControlPanel(vm);
        removeMonitoring(vm);
        releaseIp(context, vm);
        deleteAllScheduledJobsForVm(context, vm);
        deleteSupportUsersInDatabase(context, vm);
        VmAction hfsAction = deleteVmInHfs(context, vm, request);

        logger.info("Completed destroying VM {}", vm.vmId);

        Vps4DestroyDedicated.Response response = new Vps4DestroyDedicated.Response();
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

    private void releaseIp(CommandContext context, VirtualMachine vm) {
        try {
            IpAddress address = networkService.getVmPrimaryAddress(vm.vmId);
            if (address != null) {
                context.execute("Destroy-" + address.ipAddressId, ctx -> {
                    networkService.destroyIpAddress(address.ipAddressId);
                    return null;
                }, Void.class);
            }
        } catch (Exception ex) {
            // only log the exception since its not critical to stop processing the destroy
            logger.info("Primary IP record was not found for dedicated server id: {} while attempting to destroy server.", vm.vmId, ex);
        }
    }

    private void unlicenseControlPanel(VirtualMachine vm) {
        context.execute(UnlicenseControlPanel.class, vm);
    }

    public static class Response {
        public UUID vmId;
        public VmAction hfsAction;
    }

}
