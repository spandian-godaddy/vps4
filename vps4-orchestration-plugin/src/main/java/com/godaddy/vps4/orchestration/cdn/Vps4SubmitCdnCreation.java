package com.godaddy.vps4.orchestration.cdn;

import com.godaddy.vps4.cdn.CdnDataService;
import com.godaddy.vps4.cdn.CdnService;
import com.godaddy.vps4.cdn.model.CdnBypassWAF;
import com.godaddy.vps4.cdn.model.CdnClientCreateResponse;
import com.godaddy.vps4.cdn.model.CdnCacheLevel;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
import com.godaddy.vps4.orchestration.ActionCommand;
import com.godaddy.vps4.orchestration.vm.VmActionRequest;
import com.godaddy.vps4.vm.ActionService;
import com.google.inject.Inject;
import gdg.hfs.orchestration.CommandContext;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@CommandMetadata(
        name = "Vps4SubmitCdnCreation",
        requestType = Vps4SubmitCdnCreation.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SubmitCdnCreation extends ActionCommand<Vps4SubmitCdnCreation.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4SubmitCdnCreation.class);
    private final CdnService cdnService;

    private final CdnDataService cdnDataService;

    private final NetworkService networkService;
    private Request request;

    @Inject
    public Vps4SubmitCdnCreation(ActionService actionService, CdnDataService cdnDataService, CdnService cdnService,
                                 NetworkService networkService) {
        super(actionService);
        this.cdnService = cdnService;
        this.cdnDataService = cdnDataService;
        this.networkService = networkService;
    }

    public static class Request extends VmActionRequest {
        public UUID vmId;
        public UUID customerId;
        public String domain;
        public String ipAddress;
        public CdnCacheLevel cacheLevel;
        public CdnBypassWAF bypassWAF;
    }


    public IpAddress getIpAddressOfVmId() {
        IpAddress ipAddress = networkService.getActiveIpAddressOfVm(request.vmId, request.ipAddress);
        if (ipAddress == null) {
            throw new RuntimeException("Could not find ip address " + request.ipAddress
                    + " belonging to vmId " + request.vmId + " in the database");
        }
        return ipAddress;
    }

    void waitForCdnCreationJobOrFailGracefully(CommandContext context, String siteId) {
        try {
            WaitForCdnCreationJob.Request waitRequest = new WaitForCdnCreationJob.Request();
            waitRequest.siteId = siteId;
            waitRequest.vmId = request.vmId;
            waitRequest.customerId = request.customerId;
            context.execute(WaitForCdnCreationJob.class, waitRequest);
        }
        catch (Exception e) {
            logger.error("Error while waiting for cdn creation job for siteId {} for VM: {}. Error details: {}", siteId, request.vmId, e);
            try {
                logger.info("Attempting to issue deletion of cdn siteId {} of vmId {}", siteId, request.vmId);
                cdnService.deleteCdnSite(request.customerId, siteId);
            } catch (Exception ignored) {}
            throw e;
        }
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4SubmitCdnCreation.Request request) {
        this.request = request;

        IpAddress address = getIpAddressOfVmId();

        CdnClientCreateResponse response = context.execute("SubmitCreateCdn",
                                    ctx -> cdnService.createCdn(request.customerId,
                                            request.domain, address, request.cacheLevel.toString(), request.bypassWAF.toString()),
                CdnClientCreateResponse.class);

        waitForCdnCreationJobOrFailGracefully(context, response.siteId);

        cdnDataService.createCdnSite(request.vmId, address.addressId, request.domain, response.siteId);

        return null;
    }

}
