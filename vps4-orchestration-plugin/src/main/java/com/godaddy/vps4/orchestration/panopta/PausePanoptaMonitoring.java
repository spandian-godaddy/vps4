package com.godaddy.vps4.orchestration.panopta;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "PausePanoptaMonitoring",
        requestType = UUID.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class PausePanoptaMonitoring implements Command<UUID, Void> {
    private final PanoptaService panoptaService;

    @Inject
    public PausePanoptaMonitoring(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, UUID vmId) {
        panoptaService.pauseServerMonitoring(vmId);
        return null;
    }
}
