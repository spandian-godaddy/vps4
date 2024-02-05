package com.godaddy.vps4.orchestration.panopta;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.panopta.PanoptaService;

import com.godaddy.vps4.panopta.PanoptaServiceException;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "PausePanoptaMonitoring",
        requestType = PausePanoptaMonitoring.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)

public class PausePanoptaMonitoring implements Command<PausePanoptaMonitoring.Request, Void> {
    private final PanoptaService panoptaService;

    @Inject
    public PausePanoptaMonitoring(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        try {
            panoptaService.pauseServerMonitoring(request.vmId, request.shopperId);
        } catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static class Request {
        public UUID vmId;
        public String shopperId;
    }
}
