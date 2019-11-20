package com.godaddy.vps4.orchestration.monitoring;

import javax.inject.Inject;

import com.godaddy.vps4.panopta.PanoptaService;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "DeletePanoptaCustomer",
        requestType = String.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class DeletePanoptaCustomer implements Command<String, Void> {

    private final PanoptaService panoptaService;

    @Inject
    public DeletePanoptaCustomer(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, String shopperId) {
        panoptaService.deleteCustomer(shopperId);
        return null;
    }
}
