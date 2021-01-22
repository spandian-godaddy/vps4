package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaService;

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

    private final PanoptaDataService panoptaDataService;
    private final PanoptaService panoptaService;


    @Inject
    public RemovePanoptaMonitoring(PanoptaDataService panoptaDataService, PanoptaService panoptaService) {
        this.panoptaDataService = panoptaDataService;
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, UUID vmId) {
        panoptaService.deleteServer(vmId);
        panoptaDataService.setPanoptaServerDestroyed(vmId);
        return null;
    }
}
