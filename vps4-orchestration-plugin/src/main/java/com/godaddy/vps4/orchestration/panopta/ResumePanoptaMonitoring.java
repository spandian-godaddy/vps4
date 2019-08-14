package com.godaddy.vps4.orchestration.panopta;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "ResumePanoptaMonitoring",
        requestType = UUID.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class ResumePanoptaMonitoring implements Command<UUID, Void> {
    private static final Logger logger = LoggerFactory.getLogger(ResumePanoptaMonitoring.class);
    private final PanoptaService panoptaService;

    @Inject
    public ResumePanoptaMonitoring(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, UUID vmId) {
        panoptaService.resumeServerMonitoring(vmId);
        return null;
    }
}
