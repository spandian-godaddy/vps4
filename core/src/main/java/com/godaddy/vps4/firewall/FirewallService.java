package com.godaddy.vps4.firewall;

import java.util.List;
import java.util.UUID;

public interface FirewallService {

    void createFirewallSite(UUID vmId, long ipAddressId, String domain, String siteId);
    FirewallSite getFirewallSiteFromId(String siteId);
    List<FirewallSite> getActiveFirewallSitesOfVm(UUID vmId);

    void destroyFirewallSite(String siteId);
}