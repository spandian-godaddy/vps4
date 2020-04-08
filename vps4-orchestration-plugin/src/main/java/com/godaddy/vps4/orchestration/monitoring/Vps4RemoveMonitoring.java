package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "Vps4RemoveMonitoring",
        requestType = UUID.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RemoveMonitoring implements Command<UUID, Void> {

    private static final Logger logger = LoggerFactory.getLogger(Vps4RemoveMonitoring.class);

    private final NetworkService vps4NetworkService;
    private final PanoptaDataService panoptaDataService;
    private final PanoptaService panoptaService;
    private final CreditService creditService;
    private final VirtualMachineService virtualMachineService;
    private UUID vmId;
    private CommandContext context;

    @Inject
    public Vps4RemoveMonitoring(NetworkService networkService,
                                PanoptaDataService panoptaDataService,
                                PanoptaService panoptaService,
                                CreditService creditService,
                                VirtualMachineService virtualMachineService) {
        this.vps4NetworkService = networkService;
        this.panoptaDataService = panoptaDataService;
        this.panoptaService = panoptaService;
        this.creditService = creditService;
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    public Void execute(CommandContext context, UUID vmId) {
        this.context = context;
        this.vmId = vmId;

        removePanoptaMonitoring();
        removeNodePingMonitoring();
        return null;
    }

    private void removePanoptaMonitoring() {
        if (hasPanoptaMonitoring()) {
            context.execute(RemovePanoptaMonitoring.class, vmId);
            panoptaDataService.setPanoptaServerDestroyed(vmId);
            String shopperId = getShopperId(vmId);
            if (panoptaService.getActiveServers(shopperId).isEmpty()
                    && panoptaService.getSuspendedServers(shopperId).isEmpty()
                    && panoptaDataService.getActivePanoptaServers(shopperId).isEmpty()) {
                logger.info("Setting panopta customer to destroyed in panopta for shopper id {}.", shopperId);
                context.execute(DeletePanoptaCustomer.class, shopperId);
                logger.info("Setting panopta customer to destroyed in vps4 db for shopper id {}.", shopperId);
                panoptaDataService.checkAndSetPanoptaCustomerDestroyed(shopperId);
            }
        }
    }

    private String getShopperId(UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        return credit.getShopperId();
    }

    private boolean hasPanoptaMonitoring() {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        return panoptaDetails != null;
    }

    private void removeNodePingMonitoring() {
        IpAddress primaryIp = vps4NetworkService.getVmPrimaryAddress(vmId);
        if (hasNodePingMonitoring(primaryIp)) {
            context.execute(RemoveNodePingMonitoring.class, primaryIp);
        }
    }

    private boolean hasNodePingMonitoring(IpAddress primaryIp) {
        return (primaryIp != null && primaryIp.pingCheckId != null);
    }
}
