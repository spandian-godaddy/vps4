package com.godaddy.vps4.orchestration.firewall;


import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateStatusResponse;
import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallStatus;
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
        name = "WaitForFirewallCreationJob",
        requestType = WaitForFirewallCreationJob.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class WaitForFirewallCreationJob implements Command<WaitForFirewallCreationJob.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(WaitForFirewallCreationJob.class);

    private final FirewallService firewallService;
    private final Cryptography cryptography;

    @Inject
    public WaitForFirewallCreationJob(FirewallService firewallService, Cryptography cryptography) {
        this.firewallService = firewallService;
        this.cryptography = cryptography;
    }

    public static class Request {
        public String shopperId;
        public byte[] encryptedCustomerJwt;
        public String siteId;
        public UUID vmId;
    }

    public boolean isFirewallVerificationInfoPopulated(FirewallDetail firewallDetail) {
        return (firewallDetail.productData != null && firewallDetail.productData.cloudflare != null &&
                (firewallDetail.productData.cloudflare.certificateValidation != null &&
                firewallDetail.productData.cloudflare.certificateValidation != null));
    }


    @Override
    public Void execute(CommandContext context, WaitForFirewallCreationJob.Request request) {
        FirewallDetail firewallDetail;
        String customerJwt = cryptography.decrypt(request.encryptedCustomerJwt);
        do {
            firewallDetail = Utils.runWithRetriesForServerAndProcessingErrorException(context,
                                                              logger,
                                                              () -> firewallService.getFirewallSiteDetail(
                                                                      request.shopperId, customerJwt, request.siteId, request.vmId, true
                                                              ));

        } while (firewallDetail != null && firewallDetail.status != FirewallStatus.FAILED
                && !isFirewallVerificationInfoPopulated(firewallDetail));

        if (firewallDetail == null) {
            throw new RuntimeException("Failed to complete firewall creation");
        }
        if (firewallDetail.status == FirewallStatus.FAILED) {
            throw new RuntimeException(String.format("Failed to complete firewall creation status: %s", firewallDetail.status));
        }
        return null;
    }
}
