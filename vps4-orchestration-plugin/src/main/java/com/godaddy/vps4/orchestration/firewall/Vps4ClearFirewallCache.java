package com.godaddy.vps4.orchestration.firewall;

import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallClientInvalidateCacheResponse;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
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
        name = "Vps4ClearFirewallCache",
        requestType = Vps4ClearFirewallCache.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4ClearFirewallCache extends ActionCommand<Vps4ClearFirewallCache.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4ClearFirewallCache.class);
    private final FirewallDataService firewallDataService;
    private final FirewallService firewallService;
    private final Cryptography cryptography;
    private Request request;

    @Inject
    public Vps4ClearFirewallCache(ActionService actionService, FirewallDataService firewallDataService, FirewallService firewallService,
                                  Cryptography cryptography) {
        super(actionService);
        this.firewallDataService = firewallDataService;
        this.firewallService = firewallService;
        this.cryptography = cryptography;
    }

    public static class Request extends VmActionRequest {
        public UUID vmId;
        public String siteId;
        public byte[] encryptedCustomerJwt;
        public String shopperId;
    }


    public void verifyFirewallBelongsToVmId() {
        VmFirewallSite vmFirewallSite = firewallDataService.getFirewallSiteFromId(request.vmId, request.siteId);
        if (vmFirewallSite == null) {
            throw new RuntimeException("Could not find firewall siteId " + request.siteId
                    + " belonging to vmId " + request.vmId + " in the database");
        }
    }



    @Override
    protected Void executeWithAction(CommandContext context, Vps4ClearFirewallCache.Request request) {
        this.request = request;

        verifyFirewallBelongsToVmId();

        FirewallClientInvalidateCacheResponse response = context.execute("ClearFirewallCache",
                                    ctx -> firewallService.invalidateFirewallCache(request.shopperId,
                                            cryptography.decrypt(request.encryptedCustomerJwt), request.siteId),
                FirewallClientInvalidateCacheResponse.class);

        WaitForFirewallClearCacheJob.Request waitRequest = new WaitForFirewallClearCacheJob.Request();
        waitRequest.encryptedCustomerJwt = request.encryptedCustomerJwt;
        waitRequest.shopperId = request.shopperId;
        waitRequest.siteId = request.siteId;
        waitRequest.validationId = response.invalidationId;
        context.execute(WaitForFirewallClearCacheJob.class, waitRequest);
        return null;
    }

}
