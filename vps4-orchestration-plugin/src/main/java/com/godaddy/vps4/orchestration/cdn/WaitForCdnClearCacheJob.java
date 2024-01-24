package com.godaddy.vps4.orchestration.cdn;


import javax.inject.Inject;

import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateStatusResponse;
import com.godaddy.vps4.cdn.model.CdnStatus;
import com.godaddy.vps4.util.Cryptography;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.scheduler.Utils;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

@CommandMetadata(
        name = "WaitForCdnClearCacheJob",
        requestType = WaitForCdnClearCacheJob.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class WaitForCdnClearCacheJob implements Command<WaitForCdnClearCacheJob.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(WaitForCdnClearCacheJob.class);

    private final CdnService cdnService;
    private final Cryptography cryptography;

    @Inject
    public WaitForCdnClearCacheJob(CdnService cdnService, Cryptography cryptography) {
        this.cdnService = cdnService;
        this.cryptography = cryptography;
    }

    public static class Request {
        String shopperId;
        byte[] encryptedCustomerJwt;
        String siteId;
        String validationId;
    }

    @Override
    public Void execute(CommandContext context, WaitForCdnClearCacheJob.Request request) {
        CdnClientInvalidateStatusResponse statusResponse;
        String customerJwt = cryptography.decryptIgnoreNull(request.encryptedCustomerJwt);
        do {
            statusResponse = Utils.runWithRetriesForServerAndProcessingErrorException(context,
                                                              logger,
                                                              () -> cdnService.getCdnInvalidateCacheStatus(
                                                                      request.shopperId, customerJwt,
                                                                      request.siteId, request.validationId
                                                              ));
        } while (statusResponse != null && (statusResponse.status == CdnStatus.PENDING));

        if (statusResponse == null) {
            throw new RuntimeException("Failed to complete cdn cache invalidation");
        }
        if (statusResponse.status != CdnStatus.SUCCESS) {
            throw new RuntimeException(String.format("Failed to complete cdn cache invalidation status: %s, error: %s", statusResponse.status, statusResponse.message));
        }
        return null;
    }
}
