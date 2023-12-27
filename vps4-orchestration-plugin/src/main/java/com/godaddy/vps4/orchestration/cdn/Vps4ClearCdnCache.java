package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnClientInvalidateCacheResponse;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@CommandMetadata(
        name = "Vps4ClearCdnCache",
        requestType = Vps4ClearCdnCache.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ClearCdnCache extends ActionCommand<Vps4ClearCdnCache.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4ClearCdnCache.class);
    private final CdnDataService cdnDataService;
    private final CdnService cdnService;
    private final Cryptography cryptography;
    private Request request;

    @Inject
    public Vps4ClearCdnCache(ActionService actionService, CdnDataService cdnDataService, CdnService cdnService,
                             Cryptography cryptography) {
        super(actionService);
        this.cdnDataService = cdnDataService;
        this.cdnService = cdnService;
        this.cryptography = cryptography;
    }

    public static class Request extends VmActionRequest {
        public UUID vmId;
        public String siteId;
        public byte[] encryptedCustomerJwt;
        public String shopperId;
    }


    public void verifyCdnBelongsToVmId() {
        VmCdnSite vmCdnSite = cdnDataService.getCdnSiteFromId(request.vmId, request.siteId);
        if (vmCdnSite == null) {
            throw new RuntimeException("Could not find cdn siteId " + request.siteId
                    + " belonging to vmId " + request.vmId + " in the database");
        }
    }



    @Override
    protected Void executeWithAction(CommandContext context, Vps4ClearCdnCache.Request request) {
        this.request = request;

        verifyCdnBelongsToVmId();

        CdnClientInvalidateCacheResponse response = context.execute("ClearCdnCache",
                                    ctx -> cdnService.invalidateCdnCache(request.shopperId,
                                            cryptography.decrypt(request.encryptedCustomerJwt), request.siteId),
                CdnClientInvalidateCacheResponse.class);

        WaitForCdnClearCacheJob.Request waitRequest = new WaitForCdnClearCacheJob.Request();
        waitRequest.encryptedCustomerJwt = request.encryptedCustomerJwt;
        waitRequest.shopperId = request.shopperId;
        waitRequest.siteId = request.siteId;
        waitRequest.validationId = response.invalidationId;
        context.execute(WaitForCdnClearCacheJob.class, waitRequest);
        return null;
    }

}
