package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;

import javax.inject.Inject;

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

    private final PanoptaService panoptaService;

    @Inject
    public RemovePanoptaMonitoring(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, UUID vmId) {
        panoptaService.removeServerMonitoring(vmId);
        return null;
    }
}
