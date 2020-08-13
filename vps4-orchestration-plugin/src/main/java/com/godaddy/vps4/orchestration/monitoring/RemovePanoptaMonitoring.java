package com.godaddy.vps4.orchestration.monitoring;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.credit.VirtualMachineCredit;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.vm.VirtualMachine;
import com.godaddy.vps4.vm.VirtualMachineService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "RemovePanoptaMonitoring",
        requestType = UUID.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class RemovePanoptaMonitoring implements Command<UUID, Void> {

    private static final Logger logger = LoggerFactory.getLogger(RemovePanoptaMonitoring.class);

    private final CreditService creditService;
    private final PanoptaDataService panoptaDataService;
    private final PanoptaService panoptaService;
    private final VirtualMachineService virtualMachineService;

    private CommandContext context;
    private UUID vmId;
    private String shopperId;

    @Inject
    public RemovePanoptaMonitoring(CreditService creditService,
                                   PanoptaDataService panoptaDataService,
                                   PanoptaService panoptaService,
                                   VirtualMachineService virtualMachineService) {
        this.creditService = creditService;
        this.panoptaDataService = panoptaDataService;
        this.panoptaService = panoptaService;
        this.virtualMachineService = virtualMachineService;
    }

    @Override
    public Void execute(CommandContext context, UUID vmId) {
        this.context = context;
        this.vmId = vmId;
        this.shopperId = getShopperId(vmId);
        removePanoptaUsingDb();
        removePanoptaServer();
        return null;
    }

    private String getShopperId(UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        return credit.getShopperId();
    }

    /*
     * Delete any Panopta servers matching the vmId in our database. This is necessary since some of the older Panopta
     * VMs do not have a name matching their Orion GUID, and therefore will not be deleted with the Panopta API lookup.
     */
    private void removePanoptaUsingDb() {
        if (hasPanoptaMonitoring()) {
            panoptaService.removeServerMonitoring(vmId);
            panoptaDataService.setPanoptaServerDestroyed(vmId);
        }
    }

    private boolean hasPanoptaMonitoring() {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        return panoptaDetails != null;
    }

    /*
     * Delete any Panopta servers with a Panopta name matching the Orion GUID. This is necessary since failed Panopta
     * installs can leave orphaned Panopta accounts outside of our DB.
     */
    private void removePanoptaServer() {
        UUID orionGuid = virtualMachineService.getVirtualMachine(vmId).orionGuid;
        List<PanoptaServer> allServers = Stream.concat(
                panoptaService.getActiveServers(shopperId).stream(),
                panoptaService.getSuspendedServers(shopperId).stream()
        ).collect(Collectors.toList());
        List<PanoptaServer> removableServers = allServers.stream()
                                               .filter(s -> s.name.equals(orionGuid.toString()))
                                               .collect(Collectors.toList());
        for (PanoptaServer server : removableServers) {
            logger.info("Attempting to delete server {} from panopta.", server.serverId);
            panoptaService.removeServerMonitoring(server.serverId, shopperId);
        }
        if (removableServers.size() == allServers.size()) {
            removePanoptaCustomer();
        }
    }

    private void removePanoptaCustomer() {
        logger.info("Setting panopta customer to destroyed in panopta for shopper id {}.", shopperId);
        context.execute(DeletePanoptaCustomer.class, shopperId);
        logger.info("Setting panopta customer to destroyed in vps4 db for shopper id {}.", shopperId);
        panoptaDataService.checkAndSetPanoptaCustomerDestroyed(shopperId);
    }
}
