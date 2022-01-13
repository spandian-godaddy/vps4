package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.panopta.PanoptaDataService;
import com.godaddy.vps4.panopta.PanoptaMetricId;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;


@CommandMetadata(
        name="Vps4RemoveDomainMonitoring",
        requestType= Vps4RemoveDomainMonitoring.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RemoveDomainMonitoring extends ActionCommand<Vps4RemoveDomainMonitoring.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4RemoveDomainMonitoring.class);

    private final PanoptaDataService panoptaDataService;
    private final PanoptaService panoptaService;

    private CommandContext context;
    private Request request;

    @Inject
    public Vps4RemoveDomainMonitoring(ActionService actionService, PanoptaDataService panoptaDataService, PanoptaService panoptaService) {
        super(actionService);
        this.panoptaDataService = panoptaDataService;
        this.panoptaService = panoptaService;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) {
        this.context = context;
        this.request = request;
        this.deleteMonitoringCheckToFqdn();
        this.updateAdditionalFqdnList();
        this.updateDatabase();
        return null;
    }

    public void updateDatabase() {
        panoptaDataService.deletePanoptaAdditionalFqdn(request.additionalFqdn, panoptaService.getServer(request.vmId).serverId);
    }

    public void updateAdditionalFqdnList() {
        logger.info("Attempting to delete additional fqdn {} from vmId {} in panopta.", request.additionalFqdn, request.vmId);
        try {
            panoptaService.deleteAdditionalFqdnFromServer(request.vmId, request.additionalFqdn);
        }
        catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteMonitoringCheckToFqdn() {
        logger.info("Attempting to delete monitoring check of fqdn {}.", request.additionalFqdn);
        try {
            PanoptaMetricId metric = panoptaService.getNetworkIdOfAdditionalFqdn(request.vmId, request.additionalFqdn);
            panoptaService.deleteNetworkService(request.vmId, metric.id);
        }
        catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Request extends VmActionRequest {
        public UUID vmId;
        public String additionalFqdn;
    }
}