package com.godaddy.vps4.orchestration.monitoring;

import com.godaddy.vps4.panopta.PanoptaService;
import com.godaddy.vps4.panopta.PanoptaServiceException;
import com.godaddy.vps4.vm.VmOutage;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import java.util.UUID;

@CommandMetadata(
        name = "GetPanoptaOutage",
        requestType = GetPanoptaOutage.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class GetPanoptaOutage implements Command<GetPanoptaOutage.Request, VmOutage> {

    private static final Logger logger = LoggerFactory.getLogger(GetPanoptaOutage.class);
    private final PanoptaService panoptaService;

    @Inject
    public GetPanoptaOutage(PanoptaService panoptaService) {
        this.panoptaService = panoptaService;
    }

    public VmOutage getOutage(UUID vmId, long outageId) {
        VmOutage outage;
        try {
            outage = panoptaService.getOutage(vmId, outageId);
        } catch (PanoptaServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return outage;
    }

    @Override
    public VmOutage execute(CommandContext context, GetPanoptaOutage.Request request) {
        VmOutage outage;
        try {
              outage = getOutage(request.vmId, request.outageId);
        } catch (ProcessingException e) {
              logger.info("Caught Processing Error while getting panopta outage. Attempting a retry");
              outage = getOutage(request.vmId, request.outageId);
        }
        return outage;
    }

    public static class Request {
        public UUID vmId;
        public long outageId;
    }
}
