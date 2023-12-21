package com.godaddy.vps4.orchestration.firewall;


import javax.inject.Inject;

import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateStatusResponse;
import com.godaddy.vps4.firewall.model.FirewallStatus;
import com.godaddy.vps4.util.Cryptography;
import gdg.hfs.orchestration.CommandMetadata;
import gdg.hfs.orchestration.CommandRetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.orchestration.scheduler.Utils;

import gdg.hfs.orchestration.Command;
import gdg.hfs.orchestration.CommandContext;

@CommandMetadata(
        name = "WaitForFirewallClearCacheJob",
        requestType = WaitForFirewallClearCacheJob.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class WaitForFirewallClearCacheJob implements Command<WaitForFirewallClearCacheJob.Request, Void> {
    private static final Logger logger = LoggerFactory.getLogger(WaitForFirewallClearCacheJob.class);

    private final FirewallService firewallService;
    private final Cryptography cryptography;

    @Inject
    public WaitForFirewallClearCacheJob(FirewallService firewallService, Cryptography cryptography) {
        this.firewallService = firewallService;
        this.cryptography = cryptography;
    }

    public static class Request {
        String shopperId;
        byte[] encryptedCustomerJwt;
        String siteId;
        String validationId;
    }

    @Override
    public Void execute(CommandContext context, WaitForFirewallClearCacheJob.Request request) {
        FirewallClientInvalidateStatusResponse statusResponse;
        String customerJwt = cryptography.decrypt(request.encryptedCustomerJwt);
        do {
            statusResponse = Utils.runWithRetriesForServerAndProcessingErrorException(context,
                                                              logger,
                                                              () -> firewallService.getFirewallInvalidateCacheStatus(
                                                                      request.shopperId, customerJwt,
                                                                      request.siteId, request.validationId
                                                              ));
        } while (statusResponse != null && (statusResponse.status == FirewallStatus.PENDING));

        if (statusResponse == null) {
            throw new RuntimeException("Failed to complete firewall cache invalidation");
        }
        if (statusResponse.status != FirewallStatus.SUCCESS) {
            throw new RuntimeException(String.format("Failed to complete firewall cache invalidation status: %s, error: %s", statusResponse.status, statusResponse.message));
        }
        return null;
    }
}
