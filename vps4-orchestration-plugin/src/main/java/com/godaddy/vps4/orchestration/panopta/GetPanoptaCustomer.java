package com.godaddy.vps4.orchestration.panopta;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class GetPanoptaCustomer implements Command<String, GetPanoptaCustomer.Response> {

    private static final Logger logger = LoggerFactory.getLogger(CreatePanoptaCustomer.class);

    private PanoptaService panoptaService;

    public static class Response {
        PanoptaCustomer panoptaCustomer;

        public PanoptaCustomer getPanoptaCustomer() {
            return panoptaCustomer;
        }
    }

    @Inject
    public GetPanoptaCustomer(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public GetPanoptaCustomer.Response execute(CommandContext context, String shopperId) {
        logger.debug("Getting customer from panopta with shopper {}", shopperId);

        PanoptaCustomer panoptaCustomer = context.execute("GetPanoptaCustomer", ctx -> {
            return getPanoptaCustomer(shopperId);
        }, PanoptaCustomer.class);

        if (panoptaCustomer != null) {
            logger.debug("Fetched customer from panopta for shopper id {}, Customer {}", shopperId,
                        panoptaCustomer.toString());
        }
        GetPanoptaCustomer.Response response = new GetPanoptaCustomer.Response();
        response.panoptaCustomer = panoptaCustomer;
        return response;
    }

    private PanoptaCustomer getPanoptaCustomer(String shopperId) {
        return panoptaService.getCustomer(shopperId);
    }
}
