package com.godaddy.vps4.orchestration.vm;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.hfs.vm.WaitForVmAction;
import com.godaddy.vps4.util.MonitoringMeta;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import gdg.hfs.vhfs.nodeping.NodePingService;
import gdg.hfs.vhfs.vm.VmAction;
import gdg.hfs.vhfs.vm.VmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.UUID;

@CommandMetadata(
        name="Vps4DestroyDedicated",
        requestType=VmActionRequest.class,
        responseType=Vps4DestroyDedicated.Response.class,
        retryStrategy = CommandRetryStrategy.NEVER
    )
public class Vps4DestroyDedicated extends ActionCommand<VmActionRequest, Vps4DestroyDedicated.Response> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4DestroyDedicated.class);
    private final VmService vmService;
    private final NodePingService monitoringService;
    private final MonitoringMeta monitoringMeta;
    CommandContext context;

    @Inject
    public Vps4DestroyDedicated(ActionService actionService,
                                VmService vmService,
                                NodePingService monitoringService,
                                MonitoringMeta monitoringMeta) {
        super(actionService);
        this.vmService = vmService;
        this.monitoringService = monitoringService;
        this.monitoringMeta = monitoringMeta;
    }

    @Override
    public Response executeWithAction(CommandContext context, VmActionRequest request) {
        this.context = context;

        logger.info("Destroying Dedicated Server {}", request.virtualMachine.vmId);
        VirtualMachine vm = request.virtualMachine;

        unlicenseControlPanel(vm);
        deleteIpMonitoring(vm.primaryIpAddress);
        deleteAllScheduledJobsForVm(context, vm);
        deleteSupportUsersInDatabase(context, vm);
        VmAction hfsAction = deleteVmInHfs(context, vm);

        logger.info("Completed destroying VM {}", vm.vmId);

        Vps4DestroyDedicated.Response response = new Vps4DestroyDedicated.Response();
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

    private void deleteSupportUsersInDatabase(CommandContext context, VirtualMachine vm) {
        context.execute("DestroyAllSupportUsersForVm", Vps4RemoveSupportUsersFromDatabase.class, vm.vmId);
    }

    private void deleteAllScheduledJobsForVm(CommandContext context, VirtualMachine vm) {
        context.execute("DestroyAllScheduledJobsForVm", Vps4DeleteAllScheduledJobsForVm.class, vm.vmId);
    }

    private void deleteIpMonitoring(IpAddress address) {
        if (address.pingCheckId != null) {
            try {
                monitoringService.deleteCheck(monitoringMeta.getAccountId(), address.pingCheckId);
            } catch (NotFoundException ex) {
                logger.info("Monitoring check {} was not found", address.pingCheckId, ex);
            }
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
