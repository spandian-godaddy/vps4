package com.godaddy.vps4.orchestration.firewall;

import com.godaddy.vps4.firewall.FirewallDataService;
import com.godaddy.vps4.firewall.FirewallService;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
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
        name="Vps4RemoveFirewallSite",
        requestType= Vps4RemoveFirewallSite.Request.class,
        retryStrategy = CommandRetryStrategy.NEVER
)
public class Vps4RemoveFirewallSite extends ActionCommand<Vps4RemoveFirewallSite.Request, Void> {
    public static final Logger logger = LoggerFactory.getLogger(Vps4RemoveFirewallSite.class);

    private final FirewallDataService firewallDataService;
    private final FirewallService firewallService;
    private final Cryptography cryptography;

    private Request request;

    @Inject
    public Vps4RemoveFirewallSite(ActionService actionService, FirewallDataService firewallDataService, FirewallService firewallService,
                                  Cryptography cryptography) {
        super(actionService);
        this.firewallDataService = firewallDataService;
        this.firewallService = firewallService;
        this.cryptography = cryptography;
    }

    @Override
    public Void executeWithAction(CommandContext context, Request request) {
        this.request = request;
        verifyFirewallBelongsToVmId();
        issueFirewallSiteDeletion();
        updateDatabase();
        return null;
    }

    public void updateDatabase() {
        firewallDataService.destroyFirewallSite(request.vmId, request.siteId);
    }

    public void issueFirewallSiteDeletion() {
        logger.info("Attempting to issue deletion of firewall siteId {} of vmId {}", request.siteId, request.vmId);
        firewallService.deleteFirewallSite(request.shopperId,
                cryptography.decrypt(request.encryptedCustomerJwt), request.siteId);
    }

    public void verifyFirewallBelongsToVmId() {
        VmFirewallSite vmFirewallSite = firewallDataService.getFirewallSiteFromId(request.vmId, request.siteId);
        if (vmFirewallSite == null) {
            throw new RuntimeException("Could not find firewall siteId " + request.siteId
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