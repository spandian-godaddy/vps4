package com.godaddy.vps4.orchestration.monitoring;

import java.util.UUID;

import javax.inject.Inject;

import com.godaddy.vps4.credit.CreditService;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaService;

import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VirtualMachine;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;

@CommandMetadata(
        name = "RemovePanoptaMonitoring",
        requestType = RemovePanoptaMonitoring.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class RemovePanoptaMonitoring implements Command<RemovePanoptaMonitoring.Request, Void> {

    private final CreditService creditService;
    private final PanoptaDataService panoptaDataService;
    private final PanoptaService panoptaService;


    @Inject
    public RemovePanoptaMonitoring(CreditService creditService, PanoptaDataService panoptaDataService, PanoptaService panoptaService) {
        this.creditService = creditService;
        this.panoptaDataService = panoptaDataService;
        this.panoptaService = panoptaService;
    }

    @Override
    public Void execute(CommandContext context, Request request) {
        try {
            panoptaService.deleteServer(request.vmId, creditService.getVirtualMachineCredit(request.orionGuid).getShopperId());
            panoptaDataService.deleteVirtualMachineAdditionalFqdns(request.vmId);
            panoptaDataService.setPanoptaServerDestroyed(request.vmId);
        } catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static class Request {
        public UUID vmId;
        public UUID orionGuid;
    }
}
