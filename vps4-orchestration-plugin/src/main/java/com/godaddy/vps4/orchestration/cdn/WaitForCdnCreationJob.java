package com.godaddy.vps4.orchestration.cdn;


import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.orchestration.scheduler.Utils;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

@CommandMetadata(
        name = "WaitForCdnCreationJob",
        requestType = WaitForCdnCreationJob.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class WaitForCdnCreationJob implements Command<WaitForCdnCreationJob.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(WaitForCdnCreationJob.class);

    private final CdnService cdnService;

    @Inject
    public WaitForCdnCreationJob(CdnService cdnService) {
        this.cdnService = cdnService;
    }

    public static class Request {
        public UUID customerId;
        public String siteId;
        public UUID vmId;
    }

    public boolean isCdnVerificationInfoPopulated(CdnDetail cdnDetail) {
        return (cdnDetail.productData != null && cdnDetail.productData.cloudflare != null &&
                (cdnDetail.productData.cloudflare.certificateValidation != null ||
                cdnDetail.productData.cloudflare.domainValidation != null));
    }

    @Override
    public Void execute(CommandContext context, WaitForCdnCreationJob.Request request) {
        CdnDetail cdnDetail;
        do {
            cdnDetail = Utils.runWithRetriesForServerAndProcessingErrorException(context,
                                                              logger,
                                                              () -> cdnService.getCdnSiteDetail(
                                                                      request.customerId, request.siteId, request.vmId, true
                                                              ), 10000);
        } while (cdnDetail != null && cdnDetail.status != CdnStatus.FAILED
                && !isCdnVerificationInfoPopulated(cdnDetail));

        if (cdnDetail == null) {
            throw new RuntimeException("Failed to complete cdn creation");
        }
        if (cdnDetail.status == CdnStatus.FAILED) {
            throw new RuntimeException(String.format("Failed to complete cdn creation status: %s", cdnDetail.status));
        }
        return null;
    }
}
