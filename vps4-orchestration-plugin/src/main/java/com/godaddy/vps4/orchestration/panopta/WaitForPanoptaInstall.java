package com.godaddy.vps4.orchestration.panopta;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "WaitForPanoptaInstall",
        requestType = WaitForPanoptaInstall.Request.class,
        responseType = PanoptaServer.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class WaitForPanoptaInstall
        implements Command<WaitForPanoptaInstall.Request, PanoptaServer> {

    public static final Logger logger = LoggerFactory.getLogger(WaitForPanoptaInstall.class);

    private PanoptaService panoptaService;
    private long timeoutValue = 1800000; // 30 minutes
    private long sleepInterval = 5000; // 5 seconds

    @Inject
    public WaitForPanoptaInstall(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public PanoptaServer execute(CommandContext context, Request request) {
        Instant expiration = Instant.now().plus(getTimeoutValue(), ChronoUnit.MILLIS);
        PanoptaServer panoptaServer = null;

        logger.info("Started polling for panopta server details , polling till expiration: {} ", expiration.toString());

        while (Instant.now().isBefore(expiration)) {
            try {
                panoptaServer = panoptaService.getServer(request.shopperId, request.serverKey);
            } catch (PanoptaServiceException e) {
                logger.warn("Could not find server in panopta for vmid: {}, shopperid: {}, serverKey: {}",
                            request.vmId, request.shopperId, request.serverKey);
            }

            if (panoptaServer == null) {
                try {
                    logger.debug(
                            "Sleeping for {} seconds. Will check the panopta server details for vm id {} shortly ",
                            getSleepInterval(), request.vmId);
                    Thread.sleep(getSleepInterval());
                } catch (InterruptedException e) {
                    logger.debug("Interrupted while sleeping ");
                }
            } else {
                logger.debug("Panopta Server details: {}", panoptaServer.toString());
                break;
            }
        }

        if (panoptaServer == null) {
            String message =
                    String.format("Timed out trying to get server details from panopta for vm id %s ", request.vmId);
            logger.error(message);
            throw new RuntimeException(message);
        }

        return panoptaServer;
    }

    public long getTimeoutValue() {
        return timeoutValue;
    }

    public void setTimeoutValue(long timeoutValue) {
        this.timeoutValue = timeoutValue;
    }

    public long getSleepInterval() {
        return sleepInterval;
    }

    public void setSleepInterval(long sleepInterval) {
        this.sleepInterval = sleepInterval;
    }

    public static class Request {
        public UUID vmId;
        public String serverKey;
        public String shopperId;
    }
}
