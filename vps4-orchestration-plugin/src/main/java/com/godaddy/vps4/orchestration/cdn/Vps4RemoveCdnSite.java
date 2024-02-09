package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.VmCdnSite;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.util.Cryptography;
import com.godaddy.vps4.vm.ActionService;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.UUID;


@CommandMetadata(
        name="Vps4RemoveCdnSite",
        requestType= Vps4RemoveCdnSite.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RemoveCdnSite extends ActionCommand<Vps4RemoveCdnSite.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4RemoveCdnSite.class);

    private final CdnDataService cdnDataService;
    private final CdnService cdnService;
    private final Cryptography cryptography;

    private Request request;

    @Inject
    public Vps4RemoveCdnSite(ActionService actionService, CdnDataService cdnDataService, CdnService cdnService,
                             Cryptography cryptography) {
        super(actionService);
        this.cdnDataService = cdnDataService;
        this.cdnService = cdnService;
        this.cryptography = cryptography;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) {
        this.request = request;
        verifyCdnBelongsToVmId();
        issueCdnSiteDeletion();
        updateDatabase();
        return null;
    }

    public void updateDatabase() {
        cdnDataService.destroyCdnSite(request.vmId, request.siteId);
    }

    public void issueCdnSiteDeletion() {
        logger.info("Attempting to issue deletion of cdn siteId {} of vmId {}", request.siteId, request.vmId);
        cdnService.deleteCdnSite(request.shopperId,
                cryptography.decryptIgnoreNull(request.encryptedCustomerJwt), request.siteId);
    }

    public void verifyCdnBelongsToVmId() {
        VmCdnSite vmCdnSite = cdnDataService.getCdnSiteFromId(request.vmId, request.siteId);
        if (vmCdnSite == null) {
            throw new RuntimeException("Could not find cdn siteId " + request.siteId
                    + " belonging to vmId " + request.vmId + " in the database");
        }
    }

    public static class Request extends VmActionRequest {
        public UUID vmId;
        public String siteId;
        public byte[] encryptedCustomerJwt;
        public String shopperId;
    }
}