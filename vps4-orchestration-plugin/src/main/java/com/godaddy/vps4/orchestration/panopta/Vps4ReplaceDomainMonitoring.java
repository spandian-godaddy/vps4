package com.godaddy.vps4.orchestration.panopta;

import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.panopta.PanoptaMetricId;
import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.ActionService;
import com.godaddy.vps4.vm.VmMetric;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;


@CommandMetadata(
        name="Vps4ReplaceDomainMonitoring",
        requestType= Vps4ReplaceDomainMonitoring.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ReplaceDomainMonitoring extends ActionCommand<Vps4ReplaceDomainMonitoring.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4ReplaceDomainMonitoring.class);

    private final PanoptaService panoptaService;

    @Inject
    public Vps4ReplaceDomainMonitoring(ActionService actionService, PanoptaService panoptaService) {
        super(actionService);
        this.panoptaService = panoptaService;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) {
        try {
            logger.info("Replacing monitoring check of fqdn {}.", request.additionalFqdn);

            PanoptaMetricId panoptaMetricId = panoptaService.getNetworkIdOfAdditionalFqdn(request.vmId, request.additionalFqdn);
            panoptaService.deleteNetworkService(request.vmId, panoptaMetricId.id);
            panoptaService.addNetworkService(request.vmId, request.protocol, request.additionalFqdn,
                                             request.operatingSystemId, request.isManaged);
        } catch (PanoptaServiceException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static class Request extends VmActionRequest {
        public UUID vmId;
        public String additionalFqdn;
        public int operatingSystemId;
        public boolean isManaged;
        public VmMetric protocol;
    }
}