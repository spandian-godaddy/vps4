package com.godaddy.vps4.orchestration.panopta;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class CreatePanoptaCustomer implements Command<CreatePanoptaCustomer.Request, CreatePanoptaCustomer.Response> {

    private static final Logger logger = LoggerFactory.getLogger(CreatePanoptaCustomer.class);

    private PanoptaService panoptaService;

    public static class Request {
        public UUID vmId;
        public String shopperId;
    }

    public static class Response {
        PanoptaCustomer panoptaCustomer;

        public PanoptaCustomer getPanoptaCustomer() {
            return panoptaCustomer;
        }
    }

    @Inject
    public CreatePanoptaCustomer(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public CreatePanoptaCustomer.Response execute(CommandContext context, CreatePanoptaCustomer.Request request) {
        logger.debug("Creating customer in panopta for hfs vm id {} and shopper {}", request.vmId, request.shopperId);

        // execute the call to create customer in Panopta using the panopta api
        PanoptaCustomer panoptaCustomer = context.execute("CreatePanoptaCustomer", ctx -> {
            return getPanoptaCustomer(request);
        }, PanoptaCustomer.class);

        CreatePanoptaCustomer.Response response = new CreatePanoptaCustomer.Response();
        response.panoptaCustomer = panoptaCustomer;
        return response;
    }

    private PanoptaCustomer getPanoptaCustomer(Request request) {
        try {
            return panoptaService.createCustomer(request.shopperId);
        } catch (PanoptaServiceException e) {
            logger.error(e.getId(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
