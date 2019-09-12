package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaDetail;

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

    private final NetworkService vps4NetworkService;
    private final PanoptaDataService panoptaDataService;
    private CommandContext context;
    private UUID vmId;

    @Inject
    public Vps4RemoveMonitoring(NetworkService networkService,
            PanoptaDataService panoptaDataService) {
        this.vps4NetworkService = networkService;
        this.panoptaDataService = panoptaDataService;
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
            markPanoptaMonitoringDeletedInDb();
            context.execute(DeletePanoptaCustomer.class, vmId);
        }
    }

    private boolean hasPanoptaMonitoring() {
        PanoptaDetail panoptaDetails = panoptaDataService.getPanoptaDetails(vmId);
        return panoptaDetails != null;
    }

    private void markPanoptaMonitoringDeletedInDb() {
        panoptaDataService.setServerDestroyedInPanopta(vmId);
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
