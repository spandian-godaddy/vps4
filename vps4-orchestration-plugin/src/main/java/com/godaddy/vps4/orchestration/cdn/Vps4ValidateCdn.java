package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnDetail;
import com.godaddy.vps4.cdn.model.CdnStatus;
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
        name = "Vps4ValidateCdn",
        requestType = Vps4ValidateCdn.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ValidateCdn extends ActionCommand<Vps4ValidateCdn.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4ValidateCdn.class);
    private final CdnService cdnService;
    private final CdnDataService cdnDataService;
    private final Cryptography cryptography;
    private Request request;

    @Inject
    public Vps4ValidateCdn(ActionService actionService, CdnDataService cdnDataService, CdnService cdnService,
                           Cryptography cryptography) {
        super(actionService);
        this.cdnService = cdnService;
        this.cdnDataService = cdnDataService;
        this.cryptography = cryptography;
    }

    public static class Request extends VmActionRequest {
        public UUID vmId;
        public String siteId;
        public byte[] encryptedCustomerJwt;
        public String shopperId;
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4ValidateCdn.Request request) {
        this.request = request;

        verifyCdnBelongsToVmId();

        CdnDetail cdnDetail = cdnService.getCdnSiteDetail(request.shopperId,
                cryptography.decryptIgnoreNull(request.encryptedCustomerJwt), request.siteId, request.vmId, true);

        CdnStatus status = getCdnStatus(cdnDetail);

        switch(status) {
            case SUCCESS:
                return null;
            case PENDING:
                submitValidationRequestAndPoll(context, cdnDetail);
                break;
            case FAILED:
                throwErrorIfCdnStatusFailed();
                break;
        }

        return null;
    }

    public void verifyCdnBelongsToVmId() {
        VmCdnSite vmCdnSite = cdnDataService.getCdnSiteFromId(request.vmId, request.siteId);
        if (vmCdnSite == null) {
            throw new RuntimeException("Could not find cdn siteId " + request.siteId
                    + " belonging to vmId " + request.vmId + " in the database");
        }
    }

    public CdnStatus getCdnStatus(CdnDetail cdnDetail) {
        if (cdnDetail == null) {
            throw new RuntimeException("Could not find cdn siteId " + request.siteId);
        }
        return cdnDetail.status;
    }

    public void submitValidationRequestAndPoll(CommandContext context, CdnDetail cdnDetail) {
        context.execute("SubmitRequestCdnValidation", ctx -> {
            cdnService.validateCdn(request.shopperId,
                    cryptography.decryptIgnoreNull(request.encryptedCustomerJwt),
                    request.siteId);
            return null;
        }, Void.class);

        pollForValidationCompletion(context, request.siteId, cdnDetail);
    }

    void pollForValidationCompletion(CommandContext context, String siteId, CdnDetail cdnDetail) {
        WaitForCdnValidationStatusJob.Request waitRequest = new WaitForCdnValidationStatusJob.Request();
        waitRequest.encryptedCustomerJwt = request.encryptedCustomerJwt;
        waitRequest.shopperId = request.shopperId;
        waitRequest.siteId = siteId;
        waitRequest.vmId = request.vmId;
        waitRequest.certificateValidation = cdnDetail.productData.cloudflare.certificateValidation;
        waitRequest.domainValidation = cdnDetail.productData.cloudflare.domainValidation;

        context.execute(WaitForCdnValidationStatusJob.class, waitRequest);
    }

    public void throwErrorIfCdnStatusFailed() {
        logger.error("CDN status is FAILED for siteId {} for VM: {}. Attempting to clean up CDN", request.siteId, request.vmId);
        try {
            logger.info("Attempting to issue deletion of cdn siteId {} of vmId {}", request.siteId, request.vmId);
            cdnService.deleteCdnSite(request.shopperId,
                    cryptography.decryptIgnoreNull(request.encryptedCustomerJwt), request.siteId);
            cdnDataService.destroyCdnSite(request.vmId, request.siteId);
        } catch (Exception ignored) {}
        throw new RuntimeException("CDN status is FAILED for siteId " + request.siteId);
    }
}
