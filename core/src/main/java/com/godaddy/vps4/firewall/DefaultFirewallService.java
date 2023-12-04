package com.godaddy.vps4.firewall;

import com.godaddy.vps4.firewall.model.FirewallDetail;
import com.godaddy.vps4.firewall.model.FirewallSite;
import com.godaddy.vps4.sso.Vps4SsoService;
import com.godaddy.vps4.sso.models.Vps4SsoToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DefaultFirewallService implements FirewallService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultFirewallService.class);

    private final Vps4SsoService ssoService;
    private final FirewallClientService firewallClientService;

    @Inject
    public DefaultFirewallService(FirewallClientService firewallClientService,
                                  Vps4SsoService ssoService)
    {
        this.firewallClientService = firewallClientService;
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
    public FirewallSite[] getAllFirewallSites(String shopperId, String customerJwt) {
        return firewallClientService.getFirewallSites(getAuthToken(shopperId, customerJwt));
    }

    @Override
    public FirewallDetail getFirewallSiteDetail(String shopperId, String customerJwt, String siteId) {
        return firewallClientService.getFirewallSiteDetail(getAuthToken(shopperId, customerJwt), siteId);
    }
}
