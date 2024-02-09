package com.godaddy.vps4.orchestration.cdn;


import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.cdn.model.CdnValidation;
import com.godaddy.vps4.orchestration.scheduler.Utils;
import com.godaddy.vps4.util.Cryptography;
import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;

@CommandMetadata(
        name = "WaitForCdnValidationStatusJob",
        requestType = WaitForCdnValidationStatusJob.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class WaitForCdnValidationStatusJob implements Command<WaitForCdnValidationStatusJob.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(WaitForCdnValidationStatusJob.class);

    private final CdnService cdnService;

    @Inject
    public WaitForCdnValidationStatusJob(CdnService cdnService) {
        this.cdnService = cdnService;
    }

    public static class Request {
        public String siteId;
        public UUID vmId;
        public UUID customerId;
        public CdnValidation[] certificateValidation;
        public CdnValidation[] domainValidation;
    }

    private boolean wasNewVerificationInfoAdded(CdnValidation[] prevCertificateValidation, CdnValidation[] prevDomainValidation,
                                              CdnValidation[] currentCertificateValidation, CdnValidation[] currentDomainValidation) {
        boolean newDomainValidationInfoAdded = prevDomainValidation != null &&  currentDomainValidation != null
                                                && prevDomainValidation.length != currentDomainValidation.length;
        boolean newCertValidationInfoAdded = prevCertificateValidation != null &&  currentCertificateValidation != null
                                                && prevCertificateValidation.length != currentCertificateValidation.length;
        if ((prevCertificateValidation == null && currentCertificateValidation != null)
                || (prevDomainValidation == null && currentDomainValidation != null)) {
            return true;
        } else if (newDomainValidationInfoAdded || newCertValidationInfoAdded) {
            return true;
        }
        return false;
    }

    @Override
    public Void execute(CommandContext context, WaitForCdnValidationStatusJob.Request request) {
        CdnDetail cdnDetail;
        do {
            cdnDetail = Utils.runWithRetriesForServerAndProcessingErrorException(context,
                                                              logger,
                                                              () -> cdnService.getCdnSiteDetail(
                                                                      request.customerId, request.siteId, request.vmId, true
                                                              ), 20000);

        } while (cdnDetail != null && !wasNewVerificationInfoAdded(request.certificateValidation, request.domainValidation,
                cdnDetail.productData.cloudflare.certificateValidation, cdnDetail.productData.cloudflare.domainValidation)
                && cdnDetail.status == CdnStatus.PENDING);

        if (cdnDetail == null) {
            throw new RuntimeException("Failed to complete cdn validation - cdn detail returned null");
        }
        if (wasNewVerificationInfoAdded(request.certificateValidation, request.domainValidation,
                cdnDetail.productData.cloudflare.certificateValidation, cdnDetail.productData.cloudflare.domainValidation)) {
            throw new RuntimeException("Failed to complete cdn validation - more verification needed - new verification info was added");
        }
        if (cdnDetail.status != CdnStatus.SUCCESS) {
            throw new RuntimeException(String.format("Failed to complete cdn validation - cdn status: %s", cdnDetail.status));
        }
        return null;
    }

}
