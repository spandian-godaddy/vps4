package com.godaddy.vps4.orchestration.firewall;

import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.FirewallBypassWAF;
import com.godaddy.vps4.firewall.model.FirewallClientCreateResponse;
import com.godaddy.vps4.firewall.model.FirewallCacheLevel;
import com.godaddy.vps4.network.IpAddress;
import com.godaddy.vps4.network.NetworkService;
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
        name = "Vps4SubmitFirewallCreation",
        requestType = Vps4SubmitFirewallCreation.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4SubmitFirewallCreation extends ActionCommand<Vps4SubmitFirewallCreation.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4SubmitFirewallCreation.class);
    private final FirewallService firewallService;

    private final FirewallDataService firewallDataService;

    private final NetworkService networkService;
    private final Cryptography cryptography;
    private Request request;

    @Inject
    public Vps4SubmitFirewallCreation(ActionService actionService, FirewallDataService firewallDataService, FirewallService firewallService,
                                      NetworkService networkService, Cryptography cryptography) {
        super(actionService);
        this.firewallService = firewallService;
        this.firewallDataService = firewallDataService;
        this.networkService = networkService;
        this.cryptography = cryptography;
    }

    public static class Request extends VmActionRequest {
        public UUID vmId;
        public String domain;
        public String ipAddress;
        public byte[] encryptedCustomerJwt;
        public String shopperId;
        public FirewallCacheLevel cacheLevel;
        public FirewallBypassWAF bypassWAF;

    }


    public IpAddress getIpAddressOfVmId() {
        IpAddress ipAddress = networkService.getActiveIpAddressOfVm(request.vmId, request.ipAddress);
        if (ipAddress == null) {
            throw new RuntimeException("Could not find ip address " + request.ipAddress
                    + " belonging to vmId " + request.vmId + " in the database");
        }
        return ipAddress;
    }

    void waitForFirewallCreationJobOrFailGracefully(CommandContext context, String siteId) {
        try {
            WaitForFirewallCreationJob.Request waitRequest = new WaitForFirewallCreationJob.Request();
            waitRequest.encryptedCustomerJwt = request.encryptedCustomerJwt;
            waitRequest.shopperId = request.shopperId;
            waitRequest.siteId = siteId;
            waitRequest.vmId = request.vmId;
            context.execute(WaitForFirewallCreationJob.class, waitRequest);
        }
        catch (Exception e) {
            logger.error("Error while waiting for firewall creation job for siteId {} for VM: {}. Error details: {}", siteId, request.vmId, e);
            try {
                logger.info("Attempting to issue deletion of firewall siteId {} of vmId {}", siteId, request.vmId);
                firewallService.deleteFirewallSite(request.shopperId,
                        cryptography.decrypt(request.encryptedCustomerJwt), siteId);
            } catch (Exception ignored) {}
            throw e;
        }
    }

    @Override
    protected Void executeWithAction(CommandContext context, Vps4SubmitFirewallCreation.Request request) {
        this.request = request;

        IpAddress address = getIpAddressOfVmId();

        FirewallClientCreateResponse response = context.execute("SubmitCreateFirewall",
                                    ctx -> firewallService.createFirewall(request.shopperId,
                                            cryptography.decrypt(request.encryptedCustomerJwt),
                                            request.domain, address, request.cacheLevel.toString(), request.bypassWAF.toString()),
                FirewallClientCreateResponse.class);

        waitForFirewallCreationJobOrFailGracefully(context, response.siteId);

        firewallDataService.createFirewallSite(request.vmId, address.addressId, request.domain, response.siteId);

        return null;
    }

}
