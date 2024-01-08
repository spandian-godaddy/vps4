package com.godaddy.vps4.orchestration.cdn;


import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.orchestration.scheduler.Utils;
import com.godaddy.vps4.util.Cryptography;
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
    private final Cryptography cryptography;

    @Inject
    public WaitForCdnCreationJob(CdnService cdnService, Cryptography cryptography) {
        this.cdnService = cdnService;
        this.cryptography = cryptography;
    }

    public static class Request {
        public String shopperId;
        public byte[] encryptedCustomerJwt;
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
        String customerJwt = cryptography.decrypt(request.encryptedCustomerJwt);
        do {
            cdnDetail = Utils.runWithRetriesForServerAndProcessingErrorException(context,
                                                              logger,
                                                              () -> cdnService.getCdnSiteDetail(
                                                                      request.shopperId, customerJwt, request.siteId, request.vmId, true
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
