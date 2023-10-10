package com.godaddy.vps4.orchestration.panopta;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class WaitForPanoptaAgentSync implements Command<WaitForPanoptaAgentSync.Request, Void> {
    private static final long SLEEP_SECONDS = 5;
    private static final long TIMEOUT_MINUTES = 10;

    private final PanoptaService panoptaService;

    @Inject
    public WaitForPanoptaAgentSync(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext commandContext, Request request) {
        Instant expiration = Instant.now().plus(TIMEOUT_MINUTES, ChronoUnit.MINUTES);

        while (Instant.now().isBefore(expiration)) {
            if (isAgentSynced(request)) {
                return null;
            }
            try {
                Thread.sleep(SLEEP_SECONDS * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException("Panopta agent sync was interrupted for VM " + request.vmId);
            }
        }

        throw new RuntimeException("Timed out waiting for agent sync on VM " + request.vmId);
    }

    private boolean isAgentSynced(Request request) {
        PanoptaServer server = panoptaService.getServer(request.vmId);
        if (server == null) {
            throw new RuntimeException("Panopta server not found for VM " + request.vmId);
        }
        return server.agentLastSynced != null && server.agentLastSynced.isAfter(request.timeOfInstall);
    }

    public static class Request {
        public Instant timeOfInstall;
        public UUID vmId;
    }
}
