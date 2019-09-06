package com.godaddy.vps4.orchestration.panopta;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaServer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class GetPanoptaServerDetails
        implements Command<GetPanoptaServerDetails.Request, GetPanoptaServerDetails.Response> {

    public static final Logger logger = LoggerFactory.getLogger(GetPanoptaServerDetails.class);

    private PanoptaService panoptaService;
    private PanoptaDataService panoptaDataService;

    @Inject
    public GetPanoptaServerDetails(PanoptaService panoptaService, PanoptaDataService panoptaDataService) {
        this.panoptaService = panoptaService;
        this.panoptaDataService = panoptaDataService;
    }

    @Override
    public Response execute(CommandContext context, Request request) {
        long timeoutValue = 300000; // 5 minutes
        Instant expiration = Instant.now().plus(timeoutValue, ChronoUnit.MILLIS);
        PanoptaServer panoptaServer = null;

        logger.info("Started polling for panopta server details , polling till expiration: {} ", expiration.toString());

        while (Instant.now().isBefore(expiration)) {

            try {
                panoptaServer = panoptaService.getServer(request.partnerCustomerKey);
            } catch (PanoptaServiceException e) {
                logger.warn(e.getId(), e.getMessage());
            }

            if (panoptaServer == null) {
                try {
                    logger.debug("Sleeping for 5 seconds. Will check the panopta server details for vm id {} shortly ",
                                request.vmId);
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.debug("Interrupted while sleeping ");
                }
            } else {
                logger.info("Panopta Server details: {}", panoptaServer.toString());
                break;
            }
        }

        if (panoptaServer == null) {
            String message =
                    String.format("Timed out trying to get server details from panopta for vm id %s ", request.vmId);
            logger.error(message);
            throw new RuntimeException(message);
        }

        // save the panopta details (customer and server information) in the vps4 database.
        PanoptaServer finalPanoptaServer = panoptaServer;
        context.execute("CreatePanoptaDetailsInVPS4Db", ctx -> {
            panoptaDataService.createPanoptaDetails(request.vmId, request.panoptaCustomer, finalPanoptaServer);
            return null;
        }, Void.class);

        GetPanoptaServerDetails.Response response = new GetPanoptaServerDetails.Response();
        response.panoptaServer = panoptaServer;
        return response;
    }

    public static class Request {
        public String partnerCustomerKey;
        public UUID vmId;
        public PanoptaCustomer panoptaCustomer;
    }

    public static class Response {
        PanoptaServer panoptaServer;

        public PanoptaServer getPanoptaServer() {
            return panoptaServer;
        }
    }
}
