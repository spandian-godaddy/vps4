package com.godaddy.vps4.orchestration.cpanel;

import com.godaddy.vps4.cpanel.CpanelAccessDeniedException;
import com.godaddy.vps4.cpanel.CpanelTimeoutException;
import com.godaddy.vps4.cpanel.Vps4CpanelService;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class WaitForPackageInstall implements Command<WaitForPackageInstall.Request, Void> {
    private static final long SLEEP_SECONDS = 5;
    private static final long TIMEOUT_MINUTES = 20;

    private final Vps4CpanelService cPanelService;

    @Inject
    public WaitForPackageInstall(Vps4CpanelService cPanelService) {
        this.cPanelService = cPanelService;
    }

    @Override
    public Void execute(CommandContext commandContext, Request request) {
        Instant expiration = Instant.now().plus(TIMEOUT_MINUTES, ChronoUnit.MINUTES);

        while (Instant.now().isBefore(expiration)) {
            try {
                if (isBuildCompleted(request)) {
                    return null;
                }
            } catch (CpanelTimeoutException | CpanelAccessDeniedException e) {
                throw new RuntimeException("Could not retrieve rpm package update status for hfs vm {} " + request.hfsVmId);
            }
            try {
                Thread.sleep(SLEEP_SECONDS * 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException("Retrieving rpm package update status was interrupted for hfs vm {} " + request.hfsVmId);
            }
        }

        throw new RuntimeException("Timed out retrieving rpm package update status for hfs vm {} " + request.hfsVmId);
    }

    private boolean isBuildCompleted(Request request) throws CpanelTimeoutException, CpanelAccessDeniedException {
        Long activeBuilds = cPanelService.getActiveBuilds(request.hfsVmId, request.buildNumber);
        if (activeBuilds == null) {
            throw new RuntimeException("Could not retrieve rpm package update status for hfs vm {} " + request.hfsVmId);
        }
        return activeBuilds == 0;
    }

    public static class Request {
        public long hfsVmId;
        public long buildNumber;
    }
}
