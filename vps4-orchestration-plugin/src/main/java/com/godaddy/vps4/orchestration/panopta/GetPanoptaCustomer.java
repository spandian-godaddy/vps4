package com.godaddy.vps4.orchestration.panopta;

import javax.inject.Inject;

import com.godaddy.vps4.panopta.PanoptaCustomer;
import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

public class GetPanoptaCustomer implements Command<String, PanoptaCustomer> {

    private PanoptaService panoptaService;

    @Inject
    public GetPanoptaCustomer(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public PanoptaCustomer execute(CommandContext context, String shopperId) {
        return context.execute("Panopta-GetCustomer",
                ctx -> panoptaService.getCustomer(shopperId),
                PanoptaCustomer.class);
    }

}
