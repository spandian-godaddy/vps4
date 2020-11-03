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
        deleteMatchingServersFromPanopta();
        panoptaDataService.setPanoptaServerDestroyed(vmId);
        return null;
    }

    private String getShopperId(UUID vmId) {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        VirtualMachineCredit credit = creditService.getVirtualMachineCredit(vm.orionGuid);
        return credit.getShopperId();
    }

    /*
     * Delete any Panopta servers with a Panopta name matching the Orion GUID. This is necessary since failed Panopta
     * installs can leave orphaned Panopta accounts which are not tracked in our DB.
     */
    private void deleteMatchingServersFromPanopta() {
        VirtualMachine vm = virtualMachineService.getVirtualMachine(vmId);
        String orion = vm.orionGuid.toString();

        List<PanoptaServer> allServers = Stream.concat(
                panoptaService.getActiveServers(shopperId).stream(),
                panoptaService.getSuspendedServers(shopperId).stream()
        ).collect(Collectors.toList());
        long ignoredId = ignoredPanoptaServer(vm);
        List<PanoptaServer> removableServers = allServers.stream()
                                               .filter(s -> s.name.equals(orion) && s.serverId != ignoredId)
                                               .collect(Collectors.toList());
        for (PanoptaServer server : removableServers) {
            logger.info("Deleting panopta server {} for orion guid {}.", server.serverId, orion);
            panoptaService.removeServerMonitoring(server.serverId, shopperId);
        }
        if (removableServers.size() == allServers.size()) {
            removePanoptaCustomer();
        }
    }


    /*
     * It's technically possible for another (active) server to exist with the same orion guid as the one being
     * destroyed. This could happen if a server was rebuilt cross-DC and the destroy action for the old VM failed.
     * Eventually we would retry the destroy action of the old VM, and when that happens, we don't want to remove
     * Panopta from the new VM.
     */
    private long ignoredPanoptaServer(VirtualMachine vm) {
        UUID newVmId = creditService.getVirtualMachineCredit(vm.orionGuid).getProductId();
        if (vm.vmId != newVmId) {
            PanoptaDetail newVmPanoptaDetails = panoptaDataService.getPanoptaDetails(newVmId);
            if (newVmPanoptaDetails != null) {
                return newVmPanoptaDetails.getServerId();
            }
        }
        return -1;
    }

    private void removePanoptaCustomer() {
        logger.info("Setting panopta customer to destroyed in panopta for shopper id {}.", shopperId);
        context.execute(DeletePanoptaCustomer.class, shopperId);
        logger.info("Setting panopta customer to destroyed in vps4 db for shopper id {}.", shopperId);
        panoptaDataService.checkAndSetPanoptaCustomerDestroyed(shopperId);
    }
}
