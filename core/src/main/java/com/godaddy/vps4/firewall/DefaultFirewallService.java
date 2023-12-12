package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.firewall.model.VmFirewallSite;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class DefaultFirewallService implements FirewallService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultFirewallService.class);

    private final Vps4SsoService ssoService;
    private final FirewallClientService firewallClientService;
    private final FirewallDataService firewallDataService;

    @Inject
    public DefaultFirewallService(FirewallClientService firewallClientService,
                                  FirewallDataService firewallDataService,
                                  Vps4SsoService ssoService)
    {
        this.firewallClientService = firewallClientService;
        this.firewallDataService = firewallDataService;
        this.ssoService = ssoService;
    }

    protected String getAuthToken(String shopperId, String customerJwt) {
        String ssoTokenHeader = "sso-jwt ";
        if (customerJwt == null) {
            Vps4SsoToken token = ssoService.getDelegationToken("idp", shopperId);
            ssoTokenHeader += token.data;
        } else {
            ssoTokenHeader += customerJwt;
        }
        return ssoTokenHeader;
    }

    @Override
    public List<FirewallSite> getFirewallSites(String shopperId, String customerJwt, UUID vmId) {
        List<VmFirewallSite> vmFirewallSiteList = firewallDataService.getActiveFirewallSitesOfVm(vmId);
        List<FirewallSite> firewallSites =  firewallClientService.getFirewallSites(getAuthToken(shopperId, customerJwt));

        List<String> vmFirewallSiteIds = vmFirewallSiteList.stream().map(site -> site.siteId.toLowerCase()).collect(Collectors.toList());
        firewallSites = firewallSites.stream().filter(firewallSite ->
                vmFirewallSiteIds.contains(firewallSite.siteId.toLowerCase())).collect(Collectors.toList());
        return firewallSites;
    }

    @Override
    public FirewallDetail getFirewallSiteDetail(String shopperId, String customerJwt, String siteId, UUID vmId) {
        VmFirewallSite vmFirewallSite = firewallDataService.getFirewallSiteFromId(vmId, siteId);
        if (vmFirewallSite == null) {
            throw new NotFoundException("Could not find site id " + siteId + " belonging to vmId " + vmId);
        }
        return firewallClientService.getFirewallSiteDetail(getAuthToken(shopperId, customerJwt), siteId);
    }

    @Override
    public void deleteFirewallSite(String shopperId, String customerJwt, String siteId) {
        firewallClientService.deleteFirewallSite(getAuthToken(shopperId, customerJwt), siteId);
    }
}
